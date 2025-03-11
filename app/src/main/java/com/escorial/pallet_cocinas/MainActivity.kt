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
            log("EnterListener", "Enter key event - type: $type, actionId: $actionId, event: $event")
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == 5 ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                log("EnterListener", "Enter key event detected")
                when (type) {
                    "product" -> handleProduct()
                    "pallet" -> handlePallet()
                }

                v.clearFocus()
                val imm = getSystemService(InputMethodManager::class.java)
                imm.hideSoftInputFromWindow(v.windowToken, 0)

                return@OnEditorActionListener true
            }
            false
        }
    }

    private fun handleProduct() {
        log("Product", "Product: ${productEditText.text}")
        val productSerial = productEditText.text.toString()
        var products = ArrayList<Product>()
        if (selectedProductType == "COCINA") {
            log("Product", "Product type: COCINA")
            products = Product.getKitchens()
        }
        else if (selectedProductType == "TERMO/CALEFON") {
            log("Product", "Product type: TERMO/CALEFON")
            products = Product.getHeaters()
        }
        else {
            Toast.makeText(this@MainActivity, "Debe seleccionar un tipo de producto para continuar", Toast.LENGTH_LONG).show()
            return
        }
        if (productSerial.isEmpty()) {
            log("Product", "Product is empty")
            productEditText.text.clear()
            productEditText.requestFocus()
            return
        }
        if (products.isEmpty()) {
            log("Product", "Product list is empty")
            productEditText.text.clear()
            productEditText.requestFocus()
            return
        }
        if (productsList.count() == 8) {
            log("Product", "Product list is full")
            productEditText.text.clear()
            productEditText.requestFocus()
            return
        }
        val product = products.find { it.serial == productSerial }
        if (product == null) {
            log("Product", "Product not found")
            productEditText.text.clear()
            productEditText.requestFocus()
            return
        }
        if (product.palletized) {
            log("Product", "Product palletized")
            productEditText.text.clear()
            productEditText.requestFocus()
            return
        }
        if (productsList.contains(product)){
            log("Product", "Product selected before")
            productEditText.text.clear()
            productEditText.requestFocus()
            return
        }
        log("Product", "Product found")
        if (!productsList.isEmpty() && productsList.last().code != product.code) {
            log("Product", "Product type incorrect")
            productEditText.text.clear()
            productEditText.requestFocus()
            return
        }
        productsList.add(product)
        productEditText.text.clear()
        productsAdapter.notifyDataSetChanged()
        productEditText.requestFocus()
    }

    private fun handlePallet() {
        log("Pallet", "Pallet: ${palletEditText.text}")
        val pallets = Pallet.getPallets()
        val pallet = pallets.find { it.code == palletEditText.text.toString() }
        if (pallet == null) {
            log("Pallet", "Pallet not found")
            palletEditText.text.clear()
            palletEditText.requestFocus()
            return
        }
        log("Pallet", "Pallet found")
        if (pallet.products != null && pallet.products!!.isNotEmpty()) {
            for (product in pallet.products) {
                productsList.add(product)
            }
            productsAdapter.notifyDataSetChanged()
        }

        palletTextView.text = "Pallet: ${palletEditText.text} (${pallet.products?.firstOrNull()?.code ?: "vacio"})"
        palletEditText.isEnabled = false
        productEditText.isEnabled = true
        productEditText.requestFocus()
    }

    private fun submit() {
        if (productsList.isEmpty()) return
        for (product in productsList) {
            if (selectedProductType == "COCINA") Product.updateStockedKitchen(product.serial, true)
            else if (selectedProductType == "TERMO/CALEFON") Product.updateStockedHeater(product.serial, true)
        }
        Pallet.updateProductsInPallet(palletEditText.text.toString(), productsList)
        val intent = intent
        finish()
        startActivity(intent)
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