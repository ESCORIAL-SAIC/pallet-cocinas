package com.escorial.pallet_cocinas

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import androidx.core.content.edit

class LoginActivity : AppCompatActivity() {

    val sharedPreferences: SharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

    lateinit var btnLogin: Button
    lateinit var etUsername: EditText
    lateinit var etPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            startMainActivity()
            return
        }

        setContentView(R.layout.activity_login)

        loadControls()

        btnLogin.setOnClickListener {
            login(etUsername.text.toString(), etPassword.text.toString())
        }
    }

    private fun login(username: String, password: String) {
        val username = username
        val password = password

        if (username == "admin" && password == "1234") {
            sharedPreferences.edit() { putBoolean("isLoggedIn", true) }
            startMainActivity()
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun loadControls() {
        val topBar = findViewById<TopBar>(R.id.topBar)
        topBar.setUserInfo("", "")
        topBar.setLogoutButtonVisibility(false)
        btnLogin = findViewById<Button>(R.id.btnLogin)
        etUsername = findViewById<EditText>(R.id.etUsername)
        etPassword = findViewById<EditText>(R.id.etPassword)
    }
}
