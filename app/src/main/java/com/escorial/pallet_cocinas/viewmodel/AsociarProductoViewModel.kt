package com.escorial.pallet_cocinas.viewmodel

import androidx.core.content.edit
import androidx.lifecycle.viewModelScope
import com.escorial.pallet_cocinas.data.model.Pallet
import com.escorial.pallet_cocinas.data.model.Product
import com.escorial.pallet_cocinas.data.repository.PalletRepository
import com.escorial.pallet_cocinas.data.repository.ProductRepository
import com.escorial.pallet_cocinas.data.repository.SessionRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AsociarProductoUiState(
    val palletFieldEnabled: Boolean = true,
    val productFieldEnabled: Boolean = false,
    val productSpinnerEnabled: Boolean = true,
    val spinnerSelection: Int = 0,
    val isLoadingProduct: Boolean = false,
    val isLoadingPallet: Boolean = false,
    val isSubmitting: Boolean = false,
    val username: String? = null,
    val fullName: String? = null
)

sealed class AsociarProductoEvent {
    data class ShowToast(val message: String) : AsociarProductoEvent()
    data class FocusProductField(val clearFirst: Boolean) : AsociarProductoEvent()
    object ProductsListChanged : AsociarProductoEvent()
    object ResetFields : AsociarProductoEvent()
}

class AsociarProductoViewModel(
    private val palletRepository: PalletRepository,
    private val productRepository: ProductRepository,
    private val sessionRepository: SessionRepository
) : BaseViewModel() {

    // Lista mutable compartida con ProductAdapter, igual que en el diseño original
    // (el ViewModel es la fuente de verdad; las notificaciones de RecyclerView las
    // dispara la Activity en respuesta a los eventos emitidos acá).
    val productsList: ArrayList<Product> = ArrayList()

    private var selectedProductType: String = ""

    val initialPalletText: String
    val initialProductText: String

    private val _uiState = MutableStateFlow(AsociarProductoUiState())
    val uiState: StateFlow<AsociarProductoUiState> = _uiState.asStateFlow()

    private val _events = Channel<AsociarProductoEvent>(Channel.BUFFERED)
    val events: Flow<AsociarProductoEvent> = _events.receiveAsFlow()

    init {
        val prefs = sessionRepository.preferences
        initialPalletText = prefs.getString("palletText", "") ?: ""
        initialProductText = prefs.getString("productText", "") ?: ""

        val jsonList = prefs.getString("productsList", null)
        if (!jsonList.isNullOrEmpty()) {
            val type = object : TypeToken<ArrayList<Product>>() {}.type
            val restoredList: ArrayList<Product>? = Gson().fromJson(jsonList, type)
            if (restoredList != null) productsList.addAll(restoredList)
        }

        _uiState.update {
            it.copy(
                palletFieldEnabled = prefs.getBoolean("palletEditTextEnabled", true),
                productFieldEnabled = prefs.getBoolean("productEditTextEnabled", false),
                productSpinnerEnabled = prefs.getBoolean("productSpinnerEnabled", true),
                spinnerSelection = prefs.getString("selectedProductIndex", "0")!!.toInt(),
                username = sessionRepository.getUsername(),
                fullName = sessionRepository.getFullName()
            )
        }
    }

    fun onProductTypeSelected(position: Int, label: String) {
        selectedProductType = label
        sessionRepository.preferences.edit { putString("selectedProductIndex", position.toString()) }
        viewModelScope.launch { _events.send(AsociarProductoEvent.ShowToast("Seleccionaste: $label")) }
    }

    fun onProductTypeNothingSelected() {
        viewModelScope.launch { _events.send(AsociarProductoEvent.ShowToast("No seleccionaste nada")) }
    }

    fun onPalletCodeSubmitted(codigo: String) {
        if (_uiState.value.isLoadingPallet) return
        safeLaunch(onLoadingChange = { loading -> _uiState.update { it.copy(isLoadingPallet = loading) } }) {
            val pallet = palletRepository.getPalletWithProducts(codigo)
            handlePallet(pallet)
        }
    }

    private suspend fun handlePallet(pallet: Pallet) {
        val products = pallet.Products
        if (products != null && products.isNotEmpty()) {
            productsList.addAll(products)
            when (products.firstOrNull()?.type) {
                "COCINA" -> _uiState.update { it.copy(spinnerSelection = 0, productSpinnerEnabled = false) }
                "TERMOTANQUE" -> _uiState.update { it.copy(spinnerSelection = 1, productSpinnerEnabled = false) }
            }
            _events.send(AsociarProductoEvent.ProductsListChanged)
        }
        _uiState.update { it.copy(palletFieldEnabled = false, productFieldEnabled = true) }
        _events.send(AsociarProductoEvent.FocusProductField(clearFirst = false))
    }

    fun onProductSerialSubmitted(serial: String) {
        if (_uiState.value.isLoadingProduct) return
        safeLaunch(onLoadingChange = { loading -> _uiState.update { it.copy(isLoadingProduct = loading) } }) {
            if (serial.isEmpty()) {
                _events.send(AsociarProductoEvent.FocusProductField(clearFirst = true))
                throw IllegalStateException("Campo de producto vacío")
            }
            val tipo = when (selectedProductType) {
                "COCINA" -> "COCINA"
                "TERMO/CALEFON" -> "TERMOTANQUE"
                "IMPORTADO" -> "IMPORTADO"
                else -> throw IllegalStateException("Debe seleccionar un tipo de producto para continuar")
            }
            val product = productRepository.getProduct(serial, tipo)
            handleProduct(product)
        }
    }

    private suspend fun handleProduct(product: Product) {
        if (!product.isAvailable) {
            _events.send(AsociarProductoEvent.ShowToast("Producto ya palletizado"))
            _events.send(AsociarProductoEvent.FocusProductField(clearFirst = true))
            return
        }
        if (getNotDeletedProducts().count() == product.maxCantByPallet) {
            _events.send(AsociarProductoEvent.ShowToast("Cantidad máxima de productos por pallet alcanzada"))
            _events.send(AsociarProductoEvent.FocusProductField(clearFirst = true))
            return
        }
        if (productsList.contains(product)) {
            _events.send(AsociarProductoEvent.ShowToast("El producto ya fue pickeado"))
            _events.send(AsociarProductoEvent.FocusProductField(clearFirst = true))
            return
        }
        if (productsList.isNotEmpty() && productsList.last().productId != product.productId) {
            _events.send(AsociarProductoEvent.ShowToast("Tipo de producto incorrecto"))
            _events.send(AsociarProductoEvent.FocusProductField(clearFirst = true))
            return
        }
        productsList.add(product)
        _events.send(AsociarProductoEvent.ProductsListChanged)
        _events.send(AsociarProductoEvent.FocusProductField(clearFirst = true))
    }

    fun deleteProduct(item: Product) {
        val position = productsList.indexOf(item)
        if (position == -1) return
        productsList[position].deleted = true
    }

    fun restoreProduct(item: Product): Boolean {
        val position = productsList.indexOf(item)
        if (position == -1) return false
        if (getNotDeletedProducts().count() >= item.maxCantByPallet) {
            viewModelScope.launch {
                _events.send(AsociarProductoEvent.ShowToast("Cantidad máxima de productos por pallet alcanzada"))
            }
            return false
        }
        productsList[position].deleted = false
        return true
    }

    fun onSubmit(codigo: String) {
        if (_uiState.value.isSubmitting) return
        if (productsList.isEmpty()) return
        if (getNotDeletedProducts().count() != productsList.first().maxCantByPallet) {
            viewModelScope.launch { _events.send(AsociarProductoEvent.ShowToast("Debe asociar todos los productos")) }
            return
        }
        val palletPost = Pallet(
            id = UUID.randomUUID(),
            descripcion = "",
            fecha_alta = "",
            codigo = codigo,
            transferir = false,
            Products = productsList,
            Usuario = sessionRepository.getUsername(default = "") ?: ""
        )
        safeLaunch(onLoadingChange = { loading -> _uiState.update { it.copy(isSubmitting = loading) } }) {
            val response = productRepository.asociarProductos(palletPost)
            if (response.isSuccessful) {
                _events.send(AsociarProductoEvent.ShowToast("Productos asociados al pallet"))
                resetState()
                _events.send(AsociarProductoEvent.ResetFields)
            } else {
                val error = response.errorBody()?.string()
                _events.send(AsociarProductoEvent.ShowToast("Error al asociar productos al pallet. $error"))
            }
        }
    }

    fun onChangePalletClicked() {
        resetState()
        viewModelScope.launch { _events.send(AsociarProductoEvent.ResetFields) }
    }

    private fun resetState() {
        productsList.clear()
        val prefs = sessionRepository.preferences
        _uiState.update {
            it.copy(
                palletFieldEnabled = true,
                productFieldEnabled = false,
                productSpinnerEnabled = true,
                spinnerSelection = prefs.getString("selectedProductIndex", "0")!!.toInt()
            )
        }
        prefs.edit {
            remove("palletText")
            remove("productText")
            remove("productsList")
        }
    }

    fun onPause(palletText: String, productText: String) {
        val state = _uiState.value
        sessionRepository.preferences.edit {
            putString("palletText", palletText)
            putString("productText", productText)
            putBoolean("palletEditTextEnabled", state.palletFieldEnabled)
            putBoolean("productEditTextEnabled", state.productFieldEnabled)
            putBoolean("productSpinnerEnabled", state.productSpinnerEnabled)
            putString("productsList", Gson().toJson(productsList))
        }
    }

    fun logout() = sessionRepository.logout()

    private fun getNotDeletedProducts(): List<Product> = productsList.filter { !it.deleted }
}
