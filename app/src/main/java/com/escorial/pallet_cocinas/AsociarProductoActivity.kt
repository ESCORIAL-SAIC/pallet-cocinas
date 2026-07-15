package com.escorial.pallet_cocinas

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import androidx.core.content.edit
import androidx.recyclerview.widget.ItemTouchHelper
import com.escorial.pallet_cocinas.utils.apiMessage
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson

class AsociarProductoActivity : AppCompatActivity() {

    lateinit var prefs: SharedPreferences

    private var isProductRequestInProgress = false
    private var isPalletRequestInProgress = false
    private var isSubmitRequestInProgress = false

    lateinit var selectedProductType: String

    lateinit var productsRecyclerView: RecyclerView
    lateinit var productAdapter: ProductAdapter
    lateinit var productEditText: EditText
    lateinit var eanEditText: EditText
    lateinit var eanLayout: android.widget.LinearLayout
    private var currentEan: String? = null
    lateinit var palletEditText: EditText
    lateinit var productSpinner: Spinner
    lateinit var submitButton: Button
    lateinit var progressBar: ProgressBar
    lateinit var changePalletButton: AppCompatImageButton

    // Getters para releer siempre la URL vigente (puede cambiar vía config in-app).
    val api get() = ApiClient.getApiService(this)

    val palletRepository get() = PalletRepository(api)

    var productsList: ArrayList<Product> = ArrayList()
    lateinit var pickeadosTextView: TextView

    val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val item = productsList[position]

            if (direction == ItemTouchHelper.LEFT || direction == ItemTouchHelper.RIGHT) {
                if (!item.deleted)
                    swipeActionDelete(item)
                else
                    swipeActionRestore(item)
            }
        }
    }

    private fun swipeActionRestore(item: Product) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar restauración")
            .setMessage("Seguro que quieres volver a asociar este elemento?")
            .setPositiveButton("Sí") { _, _ ->
                val res = restoreItem(item)
                if (res) {
                    Snackbar.make(productsRecyclerView, "Ítem restaurado", Snackbar.LENGTH_LONG)
                        .setAction("Deshacer") {
                            deleteItem(item)
                        }
                        .show()
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                val position = productsList.indexOf(item)
                productAdapter.notifyItemChanged(position)
            }
            .setCancelable(false)
            .show()
    }

    private fun swipeActionDelete(item: Product) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("Seguro que quieres desasociar este elemento?")
            .setPositiveButton("Sí") { _, _ ->
                deleteItem(item)
                Snackbar.make(productsRecyclerView, "Ítem eliminado", Snackbar.LENGTH_LONG)
                    .setAction("Deshacer") {
                        restoreItem(item)
                    }
                    .show()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                val position = productsList.indexOf(item)
                productAdapter.notifyItemChanged(position)
            }
            .setCancelable(false)
            .show()
    }

    private fun deleteItem(item: Product) {
        val position = productsList.indexOf(item)
        productsList[position].deleted = true
        productAdapter.notifyItemChanged(position)
    }
    private fun restoreItem(item: Product): Boolean {
        val position = productsList.indexOf(item)
        if (getNotDeletedProducts().count() >= item.maxCantByPallet) {
            Toast.makeText(this@AsociarProductoActivity, "Cantidad máxima de productos por pallet alcanzada", Toast.LENGTH_LONG).show()
            productAdapter.notifyItemChanged(position)
            return false
        }
        productsList[position].deleted = false
        productAdapter.notifyItemChanged(position)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asociar_producto)

        loadControls()

        restoreUIState()

        val topBar = findViewById<TopBar>(R.id.topBar)

        val username = prefs.getString("username", "null")
        val fullName = prefs.getString("fullName", "null")

        topBar.setUserInfo(username, fullName)
        topBar.setLogoutButtonVisibility(true)
        topBar.setOnLogoutClickListener  {
            logout()
            Toast.makeText(this@AsociarProductoActivity, "Logout", Toast.LENGTH_SHORT).show()
        }
        topBar.setConfigButtonVisibility(true)
        topBar.setOnConfigClickListener {
            startActivity(Intent(this@AsociarProductoActivity, ConfigActivity::class.java))
        }

        productEditText.setOnEditorActionListener(createEnterListener("product"))
        palletEditText.setOnEditorActionListener(createEnterListener("pallet"))
        eanEditText.setOnEditorActionListener(createEnterListener("ean"))

        submitButton.setOnClickListener { submit() }

        changePalletButton.setOnClickListener { resetUIState() }

        updateEanVisibility()

        palletEditText.requestFocus()
    }

    private fun createEnterListener(type: String): TextView.OnEditorActionListener {
        return TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == 5 ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                when (type) {
                    "product" -> coroutineProduct()
                    "pallet" -> coroutinePallet()
                    "ean" -> handleEan()
                }
                v.clearFocus()
                val imm = getSystemService(InputMethodManager::class.java)
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                return@OnEditorActionListener true
            }
            false
        }
    }

    private fun handleEan() {
        val ean = eanEditText.text.toString()
        if (ean.isEmpty()) {
            Toast.makeText(this@AsociarProductoActivity, "Debe escanear el EAN", Toast.LENGTH_LONG).show()
            // Se posterga para no ser pisado por el clearFocus() síncrono del listener.
            eanEditText.post { eanEditText.requestFocus() }
            return
        }
        // El EAN se enviará al hacer el pickeo del serial; no se fija currentEan todavía.
        productEditText.post { productEditText.requestFocus() }
    }

    private fun coroutineProduct() {
        if (isProductRequestInProgress)
            return
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val productSerial = productEditText.text.toString()
                if (productSerial.isEmpty()) {
                    productEditText.text.clear()
                    productEditText.requestFocus()
                    throw Exception("Campo de producto vacío")
                }
                var eanToUse: String? = null
                var product = if (selectedProductType == "COCINA") {
                    api.getProduct(productSerial, "COCINA")
                } else if (selectedProductType == "TERMO/CALEFON") {
                    api.getProduct(productSerial, "TERMOTANQUE")
                } else if (selectedProductType == "IMPORTADO") {
                    eanToUse = currentEan ?: eanEditText.text.toString()
                    if (eanToUse.isEmpty()) {
                        Toast.makeText(this@AsociarProductoActivity, "Debe escanear el EAN", Toast.LENGTH_LONG).show()
                        eanEditText.requestFocus()
                        throw Exception("Campo de EAN vacío")
                    }
                    api.getProduct(productSerial, "IMPORTADO", eanToUse)
                }
                else {
                    throw Exception("Debe seleccionar un tipo de producto para continuar")
                }
                val added = handleProduct(product)
                // Solo fijamos/deshabilitamos el EAN si el producto se agregó realmente.
                if (added && selectedProductType == "IMPORTADO") {
                    currentEan = eanToUse
                    eanEditText.isEnabled = false
                }
            } catch (h: HttpException) {
                Toast.makeText(this@AsociarProductoActivity, "Error HTTP\n${h.apiMessage()}", Toast.LENGTH_LONG).show()
                Log.d("API_ERROR", "Error HTTP. ${h.message}")
                productEditText.requestFocus()
            } catch (i: IOException) {
                Toast.makeText(this@AsociarProductoActivity, "Error de conexion.", Toast.LENGTH_LONG).show()
                Log.d("API_ERROR", "Error de conexion. ${i.message}")
                productEditText.requestFocus()
            } catch (e: Exception) {
                Toast.makeText(this@AsociarProductoActivity, "Error al obtener datos.", Toast.LENGTH_LONG).show()
                Log.d("API_ERROR", "Error al obtener datos. ${e.message}")
                productEditText.requestFocus()
            }
            finally {
                progressBar.visibility = View.GONE
                isProductRequestInProgress = false
            }
        }
    }

    private fun handleProduct(product: Product): Boolean {
        if (!product.isAvailable) {
            Log.d("Product", "Producto ya palletizado")
            Toast.makeText(this@AsociarProductoActivity, "Producto ya palletizado", Toast.LENGTH_LONG).show()
            productEditText.text.clear()
            productEditText.requestFocus()
            return false
        }
        if (getNotDeletedProducts().count() == product.maxCantByPallet) {
            Log.d("Product", "Cantidad máxima de productos por pallet alcanzada")
            Toast.makeText(this@AsociarProductoActivity, "Cantidad máxima de productos por pallet alcanzada", Toast.LENGTH_LONG).show()
            productEditText.text.clear()
            productEditText.requestFocus()
            return false
        }
        if (productsList.contains(product)){
            Log.d("Product", "El producto ya fue pickeado")
            Toast.makeText(this@AsociarProductoActivity, "El producto ya fue pickeado", Toast.LENGTH_LONG).show()
            productEditText.text.clear()
            productEditText.requestFocus()
            return false
        }
        if (!productsList.isEmpty() && productsList.last().productId != product.productId) {
            Log.d("Product", "Tipo de producto incorrecto")
            Toast.makeText(this@AsociarProductoActivity, "Tipo de producto incorrecto", Toast.LENGTH_LONG).show()
            productEditText.text.clear()
            productEditText.requestFocus()
            return false
        }

        productsList.add(product)
        productEditText.text.clear()
        productAdapter.notifyDataSetChanged()
        productEditText.requestFocus()
        return true
    }

    private fun coroutinePallet() {
        if (isPalletRequestInProgress) return

        progressBar.visibility = View.VISIBLE
        isPalletRequestInProgress = true

        lifecycleScope.launch {
            try {
                val palletCode = palletEditText.text.toString()
                val pallet = palletRepository.getPalletWithProducts(palletCode)
                handlePallet(pallet)
            } catch (h: HttpException) {
                Toast.makeText(this@AsociarProductoActivity, "Error HTTP.\n${h.apiMessage()}", Toast.LENGTH_LONG).show()
                palletEditText.requestFocus()
            } catch (i: IOException) {
                Toast.makeText(this@AsociarProductoActivity, "Error de conexion.\n${i.message}", Toast.LENGTH_LONG).show()
                palletEditText.requestFocus()
            } catch (e: Exception) {
                Toast.makeText(this@AsociarProductoActivity, "Error al obtener datos.\n${e.message}", Toast.LENGTH_LONG).show()
                palletEditText.requestFocus()
            } finally {
                progressBar.visibility = View.GONE
                isPalletRequestInProgress = false
            }
        }
    }

    private fun handlePallet(pallet: Pallet) {
        if (pallet.Products != null && pallet.Products!!.isNotEmpty()) {
            for (product in pallet.Products)
                productsList.add(product)
            productAdapter.notifyDataSetChanged()
            // selectedProductType se fija de forma sincrónica porque el callback del
            // spinner (onItemSelected tras setSelection) es asíncrono y un serial escaneado
            // en esa ventana se procesaría con el tipo anterior.
            if (pallet.Products?.firstOrNull()?.type == "COCINA") {
                selectedProductType = "COCINA"
                productSpinner.setSelection(0)
                productSpinner.isEnabled = false
            }
            else if (pallet.Products?.firstOrNull()?.type == "TERMOTANQUE") {
                selectedProductType = "TERMO/CALEFON"
                productSpinner.setSelection(1)
                productSpinner.isEnabled = false
            }
            // ASUNCIÓN: el literal "IMPORTADO" del campo type viene del backend; verificar.
            else if (pallet.Products?.firstOrNull()?.type == "IMPORTADO") {
                selectedProductType = "IMPORTADO"
                productSpinner.setSelection(2)
                productSpinner.isEnabled = false
            }
            var product = pallet.Products?.firstOrNull()
        }

        palletEditText.isEnabled = false
        productEditText.isEnabled = true
        // setSelection() no dispara el listener del spinner de forma sincrónica, por eso
        // el tipo del pallet se determina directamente (no vía selectedProductType).
        val palletType = pallet.Products?.firstOrNull()?.type
        val isImportado = palletType == "IMPORTADO" ||
            (palletType == null && ::selectedProductType.isInitialized && selectedProductType == "IMPORTADO")
        eanLayout.visibility = if (isImportado) View.VISIBLE else View.GONE
        if (isImportado) {
            // El EAN se pide en el primer pickeo nuevo.
            eanEditText.isEnabled = true
            eanEditText.text.clear()
            currentEan = null
            eanEditText.requestFocus()
        } else {
            productEditText.requestFocus()
        }
    }

    private fun submit() {
        if (isSubmitRequestInProgress)
            return
        if (productsList.isEmpty())
            return
        if (getNotDeletedProducts().count() != productsList.first().maxCantByPallet) {
            Toast.makeText(this@AsociarProductoActivity, "Debe asociar todos los productos", Toast.LENGTH_LONG).show()
            return
        }
        isSubmitRequestInProgress = true
        progressBar.visibility = View.VISIBLE
        var palletPost = Pallet(
            id = java.util.UUID.randomUUID(),
            descripcion = "",
            fecha_alta = "",
            codigo = palletEditText.text.toString(),
            transferir = false,
            Products = productsList,
            Usuario = prefs.getString("username", "")!!
        )
        lifecycleScope.launch {
            try {
                var response = api.postPalletProducts(palletPost)
                if (response.isSuccessful) {
                    Toast.makeText(this@AsociarProductoActivity, "Productos asociados al pallet", Toast.LENGTH_LONG).show()
                    Log.d("Product", "Productos asociados al pallet. $response")
                    resetUIState()
                }
                else {
                    var error = response.errorBody()?.string()
                    Toast.makeText(this@AsociarProductoActivity, "Error al asociar productos al pallet. ${error}", Toast.LENGTH_LONG).show()
                    Log.d("Product", "Error al asociar productos al pallet. $response")
                }
            } catch (h: HttpException) {
                Toast.makeText(this@AsociarProductoActivity, "Error HTTP\n${h.apiMessage()}", Toast.LENGTH_LONG).show()
                Log.d("API_ERROR", "Error HTTP. ${h.message}")
            } catch (i: IOException) {
                Toast.makeText(this@AsociarProductoActivity, "Error de conexion.", Toast.LENGTH_LONG).show()
                Log.d("API_ERROR", "Error de conexion. ${i.message}")
            } catch (e: Exception) {
                Toast.makeText(this@AsociarProductoActivity, "Error al obtener datos.", Toast.LENGTH_LONG).show()
                Log.d("API_ERROR", "Error al obtener datos. ${e.message}")
            }
            finally {
                progressBar.visibility = View.GONE
                isSubmitRequestInProgress = false
            }
        }
    }

    private fun loadControls() {
        Log.d("LoadControls", "Loading controls")
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)

        progressBar = findViewById(R.id.progressBar)
        palletEditText = findViewById(R.id.palletEditText)
        productEditText = findViewById(R.id.productEditText)
        productEditText.isEnabled = false
        eanEditText = findViewById(R.id.eanEditText)
        eanLayout = findViewById(R.id.eanLayout)
        submitButton = findViewById(R.id.submitButton)

        productSpinner = findViewById(R.id.productSpinner)
        configProductSpinner()

        productsRecyclerView = findViewById(R.id.productsRecyclerView)
        productsRecyclerView.layoutManager = LinearLayoutManager(this)

        //COMENTE ESTO PORQUE LAS LINEAS DIVISORAS SON FEAS :d
        //var divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        //productsRecyclerView.addItemDecoration(divider)

        productAdapter = ProductAdapter(productsList)
        productsRecyclerView.adapter = productAdapter

        productAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                actualizarContadorPickeados()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                actualizarContadorPickeados()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                actualizarContadorPickeados()
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)
                actualizarContadorPickeados()
            }
        })


        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(productsRecyclerView)

        changePalletButton = findViewById(R.id.changePalletButton)

        pickeadosTextView = findViewById(R.id.pickeadosTextView)

    }

    private fun actualizarContadorPickeados() {
        val count = getNotDeletedProducts().size
        val pickeadosTextView = findViewById<TextView>(R.id.pickeadosTextView)

        if (productsList.isNotEmpty()) {
            val max = productsList.first().maxCantByPallet
            pickeadosTextView.text = "Pickeados: $count/$max"

            if (count == max) {
                pickeadosTextView.setTextColor(getColor(android.R.color.holo_green_dark))
            } else {
                pickeadosTextView.setTextColor(getColor(android.R.color.black))
            }
        } else {
            pickeadosTextView.text = "Pickeados: $count"
            pickeadosTextView.setTextColor(getColor(android.R.color.black))
        }
    }




    private fun configProductSpinner() {
        val options2 = resources.getStringArray(R.array.tipo_producto)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options2)
        adapter.setDropDownViewResource(R.layout.product_dropdown_item)

        val position = prefs.getString("selectedProductIndex", 0.toString())!!.toInt()

        // El tipo se inicializa de forma sincrónica desde prefs; el callback del spinner
        // es asíncrono y no debe ser la única fuente de verdad al restaurar estado.
        selectedProductType = options2.getOrElse(position) { options2[0] }

        // El adapter debe asignarse ANTES de setSelection: asignarlo resetea la selección
        // a 0, por lo que la selección persistida debe fijarse después.
        productSpinner.adapter = adapter
        productSpinner.setSelection(position)

        productSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedOption = parent.getItemAtPosition(position).toString()
                selectedProductType = selectedOption
                prefs.edit { putString("selectedProductIndex", position.toString()) }
                updateEanVisibility()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
    }

    private fun currentTypeOrFallback(): String {
        if (::selectedProductType.isInitialized) return selectedProductType
        val idx = prefs.getString("selectedProductIndex", "0")!!.toInt()
        return resources.getStringArray(R.array.tipo_producto).getOrElse(idx) { "" }
    }

    private fun updateEanVisibility() {
        if (!::eanLayout.isInitialized) return
        eanLayout.visibility = if (currentTypeOrFallback() == "IMPORTADO") View.VISIBLE else View.GONE
    }

    private fun resetUIState() {
        productsList.clear()
        productAdapter.notifyDataSetChanged()

        palletEditText.text.clear()
        palletEditText.isEnabled = true
        productEditText.text.clear()
        productEditText.isEnabled = false

        currentEan = null
        eanEditText.text.clear()
        eanEditText.isEnabled = true

        productSpinner.isEnabled = true
        productSpinner.setSelection(prefs.getString("selectedProductIndex", 0.toString())!!.toInt())

        updateEanVisibility()

        prefs.edit {
            remove("palletText")
            remove("productText")
            remove("productsList")
            remove("eanText")
            remove("eanEnabled")
        }
        palletEditText.requestFocus()
    }
    override fun onPause() {
        super.onPause()
        prefs.edit {
            putString("palletText", palletEditText.text.toString())
            putString("productText", productEditText.text.toString())
            putBoolean("palletEditTextEnabled", palletEditText.isEnabled)
            putBoolean("productEditTextEnabled", productEditText.isEnabled)
            putBoolean("productSpinnerEnabled", productSpinner.isEnabled)
            putString("productsList", Gson().toJson(productsList))
            putString("eanText", eanEditText.text.toString())
            putBoolean("eanEnabled", eanEditText.isEnabled)
        }
    }
    fun restoreUIState() {
        palletEditText.setText(prefs.getString("palletText", ""))
        productEditText.setText(prefs.getString("productText", ""))

        palletEditText.isEnabled = prefs.getBoolean("palletEditTextEnabled", true)
        productEditText.isEnabled = prefs.getBoolean("productEditTextEnabled", false)
        productSpinner.isEnabled = prefs.getBoolean("productSpinnerEnabled", true)

        val restoredEan = prefs.getString("eanText", "") ?: ""
        val restoredEanEnabled = prefs.getBoolean("eanEnabled", true)
        eanEditText.setText(restoredEan)
        eanEditText.isEnabled = restoredEanEnabled
        // El EAN queda fijado en la sesión sii el campo está deshabilitado y con valor.
        currentEan = if (!restoredEanEnabled && restoredEan.isNotEmpty()) restoredEan else null

        actualizarContadorPickeados()

        val jsonList = prefs.getString("productsList", null)
        if (!jsonList.isNullOrEmpty()) {
            val type = object : com.google.gson.reflect.TypeToken<ArrayList<Product>>() {}.type
            val restoredList: ArrayList<Product> = Gson().fromJson(jsonList, type)
            productsList.clear()
            productsList.addAll(restoredList)
            productAdapter.notifyDataSetChanged()
        }
    }

    private fun logout() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPreferences.edit() { remove("isLoggedIn") }

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun getNotDeletedProducts(): ArrayList<Product> {
        return ArrayList(productsList.filter { !it.deleted })
    }
}