package com.escorial.pallet_cocinas

import android.content.Intent
import android.content.SharedPreferences
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
import androidx.core.content.edit
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
    lateinit var prefs: SharedPreferences

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
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)

        val topBar = findViewById<TopBar>(R.id.topBar)

        val username = prefs.getString("username", "null")
        val fullName = prefs.getString("fullName", "null")

        topBar.setUserInfo(username, fullName)
        topBar.setLogoutButtonVisibility(true)
        topBar.setOnLogoutClickListener  {
            logout()
            Toast.makeText(this@PickeoPalletActivity, "Logout", Toast.LENGTH_SHORT).show()
        }
        api = ApiClient.getApiService(this)
        palletRepository = PalletRepository(api)
        progressBar = findViewById(R.id.progressBar)
        transferirButton = findViewById(R.id.btnTransferir)
        transferirButton.setOnClickListener { transferir() }
        palletsRecyclerView = findViewById(R.id.rvPallets)
        palletEditText = findViewById(R.id.etPallet)
        palletEditText.setOnEditorActionListener(createEnterListener("pallet"))
        palletsRecyclerView.layoutManager = LinearLayoutManager(this)
        palletAdapter = PalletAdapter(palletsList)
        palletsRecyclerView.adapter = palletAdapter
    }

    private fun logout() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPreferences.edit() { remove("isLoggedIn") }

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
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
        val products = pallet.Products ?: return
        if (products.isEmpty()) {
            Toast.makeText(this@PickeoPalletActivity, "El pallet no tiene productos asociados.", Toast.LENGTH_LONG).show()
            return
        }
        if (palletsList.contains(pallet)) {
            Toast.makeText(this@PickeoPalletActivity, "Pallet ya pickeado.", Toast.LENGTH_LONG).show()
            return
        }

        if (pallet.transferir) {
            Toast.makeText(this@PickeoPalletActivity, "Pallet ya transferido.", Toast.LENGTH_LONG).show()
            return
        }

        // 4. Agrega el pallet y actualiza la UI.
        palletsList.add(pallet)
        palletAdapter.notifyDataSetChanged()
        palletEditText.text.clear()
        palletEditText.requestFocus()
    }


    private fun transferir() {
        if (isPalletRequestInProgress)
            return

        if (palletsList.isEmpty())
            return

        isPalletRequestInProgress = true
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                var response = api.postPalletTransfer(palletsList)
                if (response.isSuccessful) {
                    Toast.makeText(this@PickeoPalletActivity, "Transferencia exitosa.", Toast.LENGTH_LONG).show()
                    palletsList.clear()
                    palletAdapter.notifyDataSetChanged()
                    palletEditText.text.clear()
                    palletEditText.requestFocus()
                }
            }
            catch (e: Exception) {
                Toast.makeText(this@PickeoPalletActivity, "Error.\n${e.message}", Toast.LENGTH_LONG).show()
            }
            finally {
                isPalletRequestInProgress = false
                progressBar.visibility = View.GONE
            }
        }
    }
}