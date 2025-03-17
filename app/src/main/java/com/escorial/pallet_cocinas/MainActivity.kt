package com.escorial.pallet_cocinas

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.ListView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MainActivity : AppCompatActivity() {

    lateinit var selectedProductType: String

    lateinit var productEditText: EditText
    lateinit var palletEditText: EditText
    lateinit var palletTextView: TextView
    lateinit var productsListView: ListView
    lateinit var productSpinner: Spinner
    lateinit var submitButton: Button

    lateinit var productsAdapter: ArrayAdapter<Product>
    var productsList: ArrayList<Product> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadControls()

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
                    Toast.makeText(this@MainActivity, "Debe seleccionar un tipo de producto para continuar", Toast.LENGTH_LONG).show()
                    throw Exception("Debe seleccionar un tipo de producto para continuar")
                }
                handleProduct(product)
            } catch (h: HttpException) {
                if(h.code() == 404)
                    Toast.makeText(this@MainActivity, "No se encontró el número de serie.", Toast.LENGTH_LONG).show()
            } catch (i: IOException) {
                Toast.makeText(this@MainActivity, "Error de conexion. ${i.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                log("API_ERROR", "Error al obtener datos. ${e.message}")
            }
        }
    }

    private fun handleProduct(product: Product) {
        if (!product.isAvailable) {
            log("Product", "Producto ya palletizado")
            productEditText.text.clear()
            productEditText.requestFocus()
            return
        }
        if (productsList.count() == product.maxCantByPallet) {
            log("Product", "Cantidad máxima de productos por pallet alcanzada")
            productEditText.text.clear()
            productEditText.requestFocus()
            return
        }
        if (productsList.contains(product)){
            log("Product", "El producto ya fue pickeado")
            productEditText.text.clear()
            productEditText.requestFocus()
            return
        }
        if (!productsList.isEmpty() && productsList.last().productId != product.productId) {
            log("Product", "Tipo de producto incorrecto")
            productEditText.text.clear()
            productEditText.requestFocus()
            return
        }

        productsList.add(product)
        productEditText.text.clear()
        productsAdapter.notifyDataSetChanged()
        productEditText.requestFocus()
    }

    private fun coroutinePallet() {
        lifecycleScope.launch {
            try {
                var palletCode = palletEditText.text.toString()
                var pallet = ApiClient.apiService.getPallet(palletCode)
                pallet.Products = ApiClient.apiService.getPalletProducts(palletCode)
                handlePallet(pallet)
            } catch (h: HttpException) {
                if(h.code() == 404)
                    log("API_ERROR", "No se encontró el número de pallet. ${h.message}")
            } catch (i: IOException) {
                log("API_ERROR", "Error de conexion. ${i.message}")
            } catch (e: Exception) {
                log("API_ERROR", "Error al obtener datos. ${e.message}")
            }
        }
    }

    private fun handlePallet(pallet: Pallet) {
        if (pallet.Products != null && pallet.Products!!.isNotEmpty()) {
            for (product in pallet.Products) {
                productsList.add(product)
            }
            productsAdapter.notifyDataSetChanged()
            if (pallet.Products?.firstOrNull()?.type == "COCINA") {
                productSpinner.setSelection(0)
                productSpinner.isEnabled = false
            }
            else if (pallet.Products?.firstOrNull()?.type == "TERMOTANQUE") {
                productSpinner.setSelection(1)
                productSpinner.isEnabled = false
            }
        }
        palletTextView.text = "Pallet: ${palletEditText.text} (${pallet.Products?.firstOrNull()?.productCode ?: "vacio"})"
        palletEditText.isEnabled = false
        productEditText.isEnabled = true
        productEditText.requestFocus()
    }

    private fun submit() {
        if (productsList.isEmpty()) return
        var palletPost = Pallet(
            id = java.util.UUID.randomUUID(),
            descripcion = "",
            fecha_alta = "",
            codigo = palletEditText.text.toString(),
            Products = productsList
        )
        lifecycleScope.launch {
            var response = ApiClient.apiService.postPalletProducts(palletPost)
            if (response.isSuccessful) {
                Toast.makeText(this@MainActivity, "Productos asociados al pallet", Toast.LENGTH_LONG).show()
                val intent = intent
                finish()
                startActivity(intent)
            }
            else {
                Toast.makeText(this@MainActivity, "Error al asociar productos al pallet", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadControls() {
        log("LoadControls", "Loading controls")
        palletEditText = findViewById(R.id.palletEditText)
        productEditText = findViewById(R.id.productEditText)
        productEditText.isEnabled = false
        palletTextView = findViewById(R.id.palletTextView)
        submitButton = findViewById(R.id.submitButton)

        productSpinner = findViewById(R.id.productSpinner)
        configProductSpinner()

        productsListView = findViewById(R.id.productsListView)
        productsAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, productsList)
        productsListView.adapter = productsAdapter
    }

    private fun configProductSpinner() {
        val options = listOf("COCINA", "TERMO/CALEFON")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        productSpinner.adapter = adapter
        productSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selectedOption = parent.getItemAtPosition(position).toString()
                Toast.makeText(this@MainActivity, "Seleccionaste: $selectedOption", Toast.LENGTH_SHORT).show()
                selectedProductType = selectedOption
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                Toast.makeText(this@MainActivity, "No seleccionaste nada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun log(title: String, message: String) {
        Log.d(title, message)
        Toast.makeText(this, "$title: $message", Toast.LENGTH_SHORT).show()
    }
}