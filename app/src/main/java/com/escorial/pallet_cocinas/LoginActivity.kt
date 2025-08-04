package com.escorial.pallet_cocinas

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.ProgressBar
import android.view.View

class LoginActivity : AppCompatActivity() {

    lateinit var sharedPreferences: SharedPreferences

    lateinit var btnLogin: Button
    lateinit var etUsername: EditText
    lateinit var etPassword: EditText
    lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        loadControls()

        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            startMainActivity()
            return
        }

    }

    private fun login(username: String, password: String) {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                var login = Login(username, password)
                var response = ApiClient.apiService.postLogin(login)
                if (response.isSuccessful) {
                    var login = response.body()
                    sharedPreferences.edit { putString("username", login?.usuario_sistema) }
                    sharedPreferences.edit { putString("fullName", login?.nombre) }
                    sharedPreferences.edit { putBoolean("isLoggedIn", true) }
                    startMainActivity()
                }
            }
            catch (e: Exception) {
                Log.d("API_ERROR", "Error al obtener datos. ${e.message}")
                Toast.makeText(this@LoginActivity, "Error al obtener datos.", Toast.LENGTH_LONG).show()
            }
            finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun loadControls() {
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        //val topBar = findViewById<TopBar>(R.id.topBar)
        //topBar.setUserInfo("", "")
        //topBar.setLogoutButtonVisibility(false)
        btnLogin = findViewById<Button>(R.id.btnLogin)
        etUsername = findViewById<EditText>(R.id.etUsername)
        etPassword = findViewById<EditText>(R.id.etPassword)
        btnLogin.setOnClickListener {
            login(etUsername.text.toString(), etPassword.text.toString())
        }
        progressBar = findViewById<ProgressBar>(R.id.progressBar)
    }
}
