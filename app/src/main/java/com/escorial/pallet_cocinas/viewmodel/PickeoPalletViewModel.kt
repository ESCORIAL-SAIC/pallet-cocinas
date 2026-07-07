package com.escorial.pallet_cocinas.viewmodel

import androidx.lifecycle.viewModelScope
import com.escorial.pallet_cocinas.data.model.Pallet
import com.escorial.pallet_cocinas.data.repository.ExpedicionRepository
import com.escorial.pallet_cocinas.data.repository.SessionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PickeoPalletUiState(
    val tipo: String,
    val isLoading: Boolean = false,
    val username: String? = null,
    val fullName: String? = null
)

sealed class PickeoPalletEvent {
    data class ShowToast(val message: String) : PickeoPalletEvent()
    object ListChanged : PickeoPalletEvent()
}

class PickeoPalletViewModel(
    private val expedicionRepository: ExpedicionRepository,
    private val sessionRepository: SessionRepository,
    tipo: String
) : BaseViewModel() {

    val palletsList: ArrayList<Pallet> = ArrayList()

    private val _uiState = MutableStateFlow(
        PickeoPalletUiState(
            tipo = tipo,
            username = sessionRepository.getUsername(),
            fullName = sessionRepository.getFullName()
        )
    )
    val uiState: StateFlow<PickeoPalletUiState> = _uiState.asStateFlow()

    private val _events = Channel<PickeoPalletEvent>(Channel.BUFFERED)
    val events: Flow<PickeoPalletEvent> = _events.receiveAsFlow()

    private var lastDeletedItem: Pallet? = null
    private var lastDeletedItemPosition: Int = -1

    fun buscarPallet(codigo: String) {
        if (_uiState.value.isLoading) return
        safeLaunch(onLoadingChange = { loading -> _uiState.update { it.copy(isLoading = loading) } }) {
            val pallet = expedicionRepository.buscarPallet(codigo)
            handlePallet(pallet)
        }
    }

    private suspend fun handlePallet(pallet: Pallet) {
        try {
            val products = pallet.Products ?: return
            if (products.isEmpty()) {
                _events.send(PickeoPalletEvent.ShowToast("El pallet no tiene productos asociados."))
                return
            }
            if (palletsList.contains(pallet)) {
                _events.send(PickeoPalletEvent.ShowToast("Pallet ya pickeado."))
                return
            }
            if (_uiState.value.tipo == "transferir" && pallet.transferir) {
                _events.send(PickeoPalletEvent.ShowToast("Pallet ya transferido."))
                return
            }
            palletsList.add(pallet)
        } finally {
            _events.send(PickeoPalletEvent.ListChanged)
        }
    }

    fun eliminarPallet(position: Int) {
        if (position !in palletsList.indices) return
        lastDeletedItem = palletsList[position]
        lastDeletedItemPosition = position
        palletsList.removeAt(position)
    }

    fun restaurarUltimoEliminado(): Int? {
        val item = lastDeletedItem
        val position = lastDeletedItemPosition
        if (item == null || position == -1) return null
        palletsList.add(position, item)
        lastDeletedItem = null
        lastDeletedItemPosition = -1
        return position
    }

    fun confirmarAccion() {
        if (_uiState.value.isLoading) {
            viewModelScope.launch { _events.send(PickeoPalletEvent.ShowToast("Ya hay una transferencia en curso.")) }
            return
        }
        if (palletsList.isEmpty()) {
            viewModelScope.launch { _events.send(PickeoPalletEvent.ShowToast("No hay pallets seleccionados.")) }
            return
        }
        val tipo = _uiState.value.tipo
        safeLaunch(onLoadingChange = { loading -> _uiState.update { it.copy(isLoading = loading) } }) {
            val response = when (tipo) {
                "transferir" -> expedicionRepository.transferirPallets(palletsList)
                "desasociar" -> expedicionRepository.desasociarPallets(palletsList)
                else -> return@safeLaunch
            }
            if (response.isSuccessful) {
                val message = if (tipo == "transferir") "Transferencia" else "Desasociación"
                palletsList.clear()
                _events.send(PickeoPalletEvent.ShowToast("$message exitosa."))
                _events.send(PickeoPalletEvent.ListChanged)
            }
        }
    }

    fun logout() = sessionRepository.logout()
}
