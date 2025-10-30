package com.escorial.pallet_cocinas

import android.content.Intent
import android.content.SharedPreferences
import retrofit2.HttpException
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
import androidx.appcompat.widget.AppCompatImageButton
import com.escorial.pallet_cocinas.utils.apiMessage

class LoginActivity : AppCompatActivity() {

    lateinit var sharedPreferences: SharedPreferences

    lateinit var btnLogin: Button
    lateinit var etUsername: EditText
    lateinit var etPassword: EditText
    lateinit var progressBar: ProgressBar

    val api get() = ApiClient.getApiService(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        loadControls()

        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            val loggedUser = sharedPreferences.getString("username", "")
            if (loggedUser == "expedicion")
                startMainActivity(true)
            else
                startMainActivity(false)
            return
        }

    }

    private fun login(username: String, password: String) {
        progressBar.visibility = View.VISIBLE
        if (username == "expedicion" && password == "expedicion") {
            saveLoggedUserData("expedicion", "Expedicion")
            startMainActivity(true)
            return
        }
        lifecycleScope.launch {
            try {
                var login = Login(username, password)
                var response = api.postLogin(login)
                if (response.isSuccessful) {
                    var login = response.body()
                    saveLoggedUserData(login!!.usuario_sistema, login.nombre)
                    startMainActivity(false)
                }
            }
            catch (h: HttpException) {
                Toast.makeText(this@LoginActivity, "Error HTTP\n${h.apiMessage()}", Toast.LENGTH_LONG).show()
                Log.d("API_ERROR", "Error HTTP. ${h.message}")
            }
            catch (e: Exception) {
                Log.d("API_ERROR", "Excepcion no controlada.\n${e.message}")
                Toast.makeText(this@LoginActivity, "Error al obtener datos.", Toast.LENGTH_LONG).show()
            }
            finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun startMainActivity(bypass: Boolean) {
        val intent: Intent = if (bypass)
            Intent(this, ExpedicionActivity::class.java)
        else
            Intent(this, AsociarProductoActivity::class.java)
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

        //config button
        val configButton = findViewById<AppCompatImageButton>(R.id.btnConfiguracion)
        configButton.setOnClickListener {
            val intent = Intent(this, ConfigActivity::class.java)
            startActivity(intent)
        }
    }

    private fun saveLoggedUserData(username: String, fullName: String) {
        sharedPreferences.edit { putString("username", username) }
        sharedPreferences.edit { putString("fullName", fullName) }
        sharedPreferences.edit { putBoolean("isLoggedIn", true) }
    }
}
