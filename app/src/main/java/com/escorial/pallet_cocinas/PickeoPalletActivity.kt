package com.escorial.pallet_cocinas

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.escorial.pallet_cocinas.utils.apiMessage
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class PickeoPalletActivity : AppCompatActivity() {

    var isPalletRequestInProgress = false
    lateinit var transferirButton: Button
    lateinit var palletEditText: EditText
    lateinit var palletsRecyclerView: RecyclerView
    lateinit var progressBar: ProgressBar
    lateinit var api: ApiService
    var palletsList: ArrayList<Pallet> = ArrayList()
    lateinit var palletRepository: PalletRepository
    lateinit var palletAdapter: PalletAdapter




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pickeo_pallet)
        loadControls()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadControls() {
        api = ApiClient.getApiService(this)
        palletRepository = PalletRepository(api)
        progressBar = findViewById(R.id.progressBar)
        transferirButton = findViewById(R.id.btnTransferir)
        palletsRecyclerView = findViewById(R.id.rvPallets)
        palletEditText = findViewById(R.id.etPallet)
        palletEditText.setOnEditorActionListener(createEnterListener("pallet"))
        palletsRecyclerView.layoutManager = LinearLayoutManager(this)
        palletAdapter = PalletAdapter(palletsList)
        palletsRecyclerView.adapter = palletAdapter
    }

    private fun createEnterListener(type: String): TextView.OnEditorActionListener {
        return TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == 5 ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                when (type) {
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
                Toast.makeText(this@PickeoPalletActivity, "Error HTTP.\n${h.apiMessage()}", Toast.LENGTH_LONG).show()
            } catch (i: IOException) {
                Toast.makeText(this@PickeoPalletActivity, "Error de conexion.\n${i.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@PickeoPalletActivity, "Error al obtener datos.\n${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
                isPalletRequestInProgress = false
            }
        }
    }

    private fun handlePallet(pallet: Pallet) {

        if (pallet == null) return

        if (palletsList.contains(pallet)) {
            Toast.makeText(this@PickeoPalletActivity, "Pallet ya pickeado.", Toast.LENGTH_LONG)
            return
        }

        palletsList.add(pallet)

        palletEditText.clearFocus()
        palletEditText.requestFocus()

        /*if (pallet.Products != null && pallet.Products!!.isNotEmpty()) {
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
        }

        palletEditText.isEnabled = false
        productEditText.isEnabled = true
        productEditText.requestFocus() */
    }
}