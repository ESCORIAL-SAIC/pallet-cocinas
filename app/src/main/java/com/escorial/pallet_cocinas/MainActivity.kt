package com.escorial.pallet_cocinas

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.ListView
import android.widget.ArrayAdapter

class MainActivity : AppCompatActivity() {

    lateinit var kitchenEditText: EditText
    lateinit var palletEditText: EditText
    lateinit var palletTextView: TextView
    lateinit var kitchensListView: ListView
    lateinit var kitchensAdapter: ArrayAdapter<String>
    var kitchensList: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadControls()

        kitchenEditText.setOnEditorActionListener(createEnterListener("kitchen"))
        palletEditText.setOnEditorActionListener(createEnterListener("pallet"))

        palletEditText.requestFocus()
    }

    private fun createEnterListener(type: String): TextView.OnEditorActionListener {
        return TextView.OnEditorActionListener { v, actionId, event ->
            Log.d("EnterListener", "Enter key event - type: $type, actionId: $actionId, event: $event")
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == 5 ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                Log.d("EnterListener", "Enter key event detected")
                when (type) {
                    "kitchen" -> handleKitchen()
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

    private fun handleKitchen() {
        Log.d("Kitchen", "Kitchen: ${kitchenEditText.text}")
        val kitchenCode = kitchenEditText.text.toString()
        var kitchens = Kitchen.getSampleKitchens()
        if (kitchenCode.isEmpty()) {
            Log.d("Kitchen", "Kitchen is empty")
            kitchenEditText.text.clear()
            kitchenEditText.requestFocus()
            return
        }
        if (kitchens.isEmpty()) {
            Log.d("Kitchen", "Kitchen list is empty")
            kitchenEditText.text.clear()
            kitchenEditText.requestFocus()
            return
        }
        if (kitchensList.count() == 8) {
            Log.d("Kitchen", "Kitchen list is full")
            kitchenEditText.text.clear()
            kitchenEditText.requestFocus()
            return
        }
        if (kitchens.find { it.code == kitchenCode } == null) {
            Log.d("Kitchen", "Kitchen not found")
            kitchenEditText.text.clear()
            kitchenEditText.requestFocus()
            return
        }
        Log.d("Kitchen", "Kitchen found")
        kitchensList.add(kitchenCode)
        kitchenEditText.text.clear()
        kitchensAdapter.notifyDataSetChanged()
        kitchenEditText.requestFocus()
    }

    private fun handlePallet() {
        Log.d("Pallet", "Pallet: ${palletEditText.text}")
        val pallets = Pallet.getSamplePallets()
        if (pallets.find { it.code == palletEditText.text.toString() } == null) {
            Log.d("Pallet", "Pallet not found")
            palletEditText.text.clear()
            palletEditText.requestFocus()
            return
        }
        Log.d("Pallet", "Pallet found")
        palletTextView.text = "Pallet: ${palletEditText.text}"
        palletEditText.isEnabled = false
        kitchenEditText.isEnabled = true
        kitchenEditText.requestFocus()
    }

    private fun loadControls() {
        Log.d("LoadControls", "Loading controls")
        palletEditText = findViewById(R.id.palletEditText)
        kitchenEditText = findViewById(R.id.kitchenEditText)
        kitchenEditText.isEnabled = false
        palletTextView = findViewById(R.id.palletTextView)
        kitchensAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, kitchensList)
        kitchensListView = findViewById(R.id.kitchensListView)
        kitchensListView.adapter = kitchensAdapter
    }
}