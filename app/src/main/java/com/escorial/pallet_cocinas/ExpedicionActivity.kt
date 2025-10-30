package com.escorial.pallet_cocinas

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ExpedicionActivity : AppCompatActivity() {

    lateinit var btnTransferir: Button
    lateinit var btnDesasociar: Button
    lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_expedicion)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadControls()
    }

    private fun loadControls() {
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)

        val username = prefs.getString("username", "null")
        val fullName = prefs.getString("fullName", "null")
        val topBar = findViewById<TopBar>(R.id.topBar)

        topBar.setUserInfo(username, fullName)
        topBar.setLogoutButtonVisibility(true)
        topBar.setOnLogoutClickListener  {
            logout()
            Toast.makeText(this@ExpedicionActivity, "Logout", Toast.LENGTH_SHORT).show()
        }
        btnTransferir = findViewById(R.id.btnTransferir)
        btnDesasociar = findViewById(R.id.btnDesasociar)
        btnDesasociar.setOnClickListener { startPickeoPalletActivity("desasociar") }
        btnTransferir.setOnClickListener { startPickeoPalletActivity("transferir") }
    }

    private fun logout() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPreferences.edit() { remove("isLoggedIn") }

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startPickeoPalletActivity(tipo: String) {
        val intent = Intent(this, PickeoPalletActivity::class.java)
        intent.putExtra("tipo", tipo)
        startActivity(intent)
    }
}