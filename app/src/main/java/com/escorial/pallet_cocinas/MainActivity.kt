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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import androidx.core.content.edit
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    lateinit var sharedPreferences: SharedPreferences

    private var isProductRequestInProgress = false
    private var isPalletRequestInProgress = false
    private var isSubmitRequestInProgress = false

    lateinit var selectedProductType: String

    lateinit var productsRecyclerView: RecyclerView
    lateinit var productAdapter: ProductAdapter
    lateinit var productEditText: EditText
    lateinit var palletEditText: EditText
    lateinit var palletTextView: TextView
    lateinit var productTextView: TextView
    lateinit var productSpinner: Spinner
    lateinit var submitButton: Button
    lateinit var progressBar: ProgressBar

    var productsList: ArrayList<Product> = ArrayList()

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
                restoreItem(item)
            }
            .setNegativeButton("Cancelar") { _, _ ->
                val position = productsList.indexOf(item)
                productAdapter.notifyItemChanged(position)
            }
            .setCancelable(false)
            .show()

        Snackbar.make(productsRecyclerView, "Ítem restaurado", Snackbar.LENGTH_LONG)
            .setAction("Deshacer") {
                deleteItem(item)
            }
            .show()
    }

    private fun swipeActionDelete(item: Product) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("Seguro que quieres desasociar este elemento?")
            .setPositiveButton("Sí") { _, _ ->
                deleteItem(item)
            }
            .setNegativeButton("Cancelar") { _, _ ->
                val position = productsList.indexOf(item)
                productAdapter.notifyItemChanged(position)
            }
            .setCancelable(false)
            .show()

        Snackbar.make(productsRecyclerView, "Ítem eliminado", Snackbar.LENGTH_LONG)
            .setAction("Deshacer") {
                restoreItem(item)
            }
            .show()
    }

    private fun deleteItem(item: Product) {
        val position = productsList.indexOf(item)
        productsList[position].deleted = true
        productAdapter.notifyItemChanged(position)
    }

    private fun restoreItem(item: Product) {
        val position = productsList.indexOf(item)
        productsList[position].deleted = false
        productAdapter.notifyItemChanged(position)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadControls()

        val topBar = findViewById<TopBar>(R.id.topBar)

        val username = sharedPreferences.getString("username", "null")
        val fullName = sharedPreferences.getString("fullName", "null")

        topBar.setUserInfo(username, fullName)
        topBar.setLogoutButtonVisibility(true)
        topBar.setOnLogoutClickListener  {
            logout()
            Toast.makeText(this@MainActivity, "Logout", Toast.LENGTH_SHORT).show()
        }

        productEditText.setOnEditorActionListener(createEnterListener("product"))
        palletEditText.setOnEditorActionListener(createEnterListener("pallet"))

        submitButton.setOnClickListener { submit() }

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
                }
                v.clearFocus()
                val imm = getSystemService(InputMethodManager::class.java)
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                return@OnEditorActionListener true
            }
            false
        }
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
                var product = if (selectedProductType == "COCINA") {
                    ApiClient.apiService.getKitchen(productSerial.toInt())
                } else if (selectedProductType == "TERMO/CALEFON") {
                    ApiClient.apiService.getHeater(productSerial.toInt())
                } else {
                    throw Exception("Debe seleccionar un tipo de producto para continuar")
                }
                handleProduct(product)
            } catch (h: HttpException) {
                if(h.code() == 404)
                    Toast.makeText(this@MainActivity, "No se encontró el número de serie.", Toast.LENGTH_LONG).show()
                else
                    Toast.makeText(this@MainActivity, "Error HTTP.", Toast.LENGTH_LONG).show()
                Log.d("API_ERROR", "Error HTTP. ${h.message}")
            } catch (i: IOException) {
                Toast.makeText(this@MainActivity, "Error de conexion.", Toast.LENGTH_LONG).show()
                Log.d("API_ERROR", "Error de conexion. ${i.message}")
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error al obtener datos.", Toast.LENGTH_LONG).show()
                Log.d("API_ERROR", "Error al obtener datos. ${e.message}")
            }
            finally {
                progressBar.visibility = View.GONE
                isProductRequestInProgress = false
            }
        }
    }

    private fun handleProduct(product: Product) {
        if (!product.isAvailable) {
            Log.d("Product", "Producto ya palletizado")
            Toast.makeText(this@MainActivity, "Producto ya palletizado", Toast.LENGTH_LONG).show()
            productEditText.text.clear()
            productEditText.requestFocus()
            return
        }
        if (productsList.count() == product.maxCantByPallet) {
            Log.d("Product", "Cantidad máxima de productos por pallet alcanzada")
            Toast.makeText(this@MainActivity, "Cantidad máxima de productos por pallet alcanzada", Toast.LENGTH_LONG).show()
            productEditText.text.clear()
            productEditText.requestFocus()
            return
        }
        if (productsList.contains(product)){
            Log.d("Product", "El producto ya fue pickeado")
            Toast.makeText(this@MainActivity, "El producto ya fue pickeado", Toast.LENGTH_LONG).show()
            productEditText.text.clear()
            productEditText.requestFocus()
            return
        }
        if (!productsList.isEmpty() && productsList.last().productId != product.productId) {
            Log.d("Product", "Tipo de producto incorrecto")
            Toast.makeText(this@MainActivity, "Tipo de producto incorrecto", Toast.LENGTH_LONG).show()
            productEditText.text.clear()
            productEditText.requestFocus()
            return
        }

        productsList.add(product)
        productEditText.text.clear()
        productAdapter.notifyDataSetChanged()
        productEditText.requestFocus()
    }

    private fun coroutinePallet() {
        if (isPalletRequestInProgress)
            return
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                var palletCode = palletEditText.text.toString()
                var pallet = ApiClient.apiService.getPallet(palletCode)
                pallet.Products = ApiClient.apiService.getPalletProducts(palletCode)
                handlePallet(pallet)
            } catch (h: HttpException) {
                if(h.code() == 404)
                    Toast.makeText(this@MainActivity, "No se encontró el número de pallet.", Toast.LENGTH_LONG).show()
                else
                    Toast.makeText(this@MainActivity, "Error HTTP.", Toast.LENGTH_LONG).show()
                Log.d("API_ERROR", "No se encontró el número de pallet. ${h.message}")
            } catch (i: IOException) {
                Toast.makeText(this@MainActivity, "Error de conexion.", Toast.LENGTH_LONG).show()
                Log.d("API_ERROR", "Error de conexion. ${i.message}")
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error al obtener datos.", Toast.LENGTH_LONG).show()
                Log.d("API_ERROR", "Error al obtener datos. ${e.message}")
            }
            finally {
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
            if (pallet.Products?.firstOrNull()?.type == "COCINA") {
                productSpinner.setSelection(0)
                productSpinner.isEnabled = false
            }
            else if (pallet.Products?.firstOrNull()?.type == "TERMOTANQUE") {
                productSpinner.setSelection(1)
                productSpinner.isEnabled = false
            }
            var product = pallet.Products?.firstOrNull()
            productTextView.text = "Producto: ${product?.productCode} - ${product?.description} (${product?.type})"
        }
        palletTextView.text = "Pallet: ${palletEditText.text}"
        palletEditText.isEnabled = false
        productEditText.isEnabled = true
        productEditText.requestFocus()
    }

    private fun submit() {
        if (isSubmitRequestInProgress)
            return
        if (productsList.isEmpty())
            return
        progressBar.visibility = View.VISIBLE
        var palletPost = Pallet(
            id = java.util.UUID.randomUUID(),
            descripcion = "",
            fecha_alta = "",
            codigo = palletEditText.text.toString(),
            Products = productsList,
            Usuario = sharedPreferences.getString("username", "")!!
        )
        lifecycleScope.launch {
            try {
                var response = ApiClient.apiService.postPalletProducts(palletPost)
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Productos asociados al pallet", Toast.LENGTH_LONG).show()
                    Log.d("Product", "Productos asociados al pallet. $response")
                    resetUIState()
                }
                else {
                    var error = response.errorBody()?.string()
                    Toast.makeText(this@MainActivity, "Error al asociar productos al pallet. ${error}", Toast.LENGTH_LONG).show()
                    Log.d("Product", "Error al asociar productos al pallet. $response")
                }
            } catch (h: HttpException) {
                Toast.makeText(this@MainActivity, "Error HTTP.", Toast.LENGTH_LONG).show()
                Log.d("API_ERROR", "Error HTTP. ${h.message}")
            } catch (i: IOException) {
                Toast.makeText(this@MainActivity, "Error de conexion.", Toast.LENGTH_LONG).show()
                Log.d("API_ERROR", "Error de conexion. ${i.message}")
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error al obtener datos.", Toast.LENGTH_LONG).show()
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
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        progressBar = findViewById(R.id.progressBar)
        palletEditText = findViewById(R.id.palletEditText)
        productEditText = findViewById(R.id.productEditText)
        productEditText.isEnabled = false
        palletTextView = findViewById(R.id.palletTextView)
        productTextView = findViewById(R.id.productTextView)
        submitButton = findViewById(R.id.submitButton)

        productSpinner = findViewById(R.id.productSpinner)
        configProductSpinner()

        productsRecyclerView = findViewById(R.id.productsRecyclerView)
        productsRecyclerView.layoutManager = LinearLayoutManager(this)

        var divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        productsRecyclerView.addItemDecoration(divider)

        productAdapter = ProductAdapter(productsList)
        productsRecyclerView.adapter = productAdapter

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(productsRecyclerView)
    }

    private fun configProductSpinner() {
        val options = listOf("COCINA", "TERMO/CALEFON")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        productSpinner.adapter = adapter
        productSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedOption = parent.getItemAtPosition(position).toString()
                Toast.makeText(this@MainActivity, "Seleccionaste: $selectedOption", Toast.LENGTH_SHORT).show()
                selectedProductType = selectedOption
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                Toast.makeText(this@MainActivity, "No seleccionaste nada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetUIState() {
        productsList.clear()
        productAdapter.notifyDataSetChanged()

        palletEditText.text.clear()
        palletEditText.isEnabled = true
        productEditText.text.clear()
        productEditText.isEnabled = false
        palletTextView.text = "Pallet: "

        productSpinner.isEnabled = true
        productSpinner.setSelection(0)

        palletEditText.requestFocus()
    }

    private fun logout() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPreferences.edit() { remove("isLoggedIn") }

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}