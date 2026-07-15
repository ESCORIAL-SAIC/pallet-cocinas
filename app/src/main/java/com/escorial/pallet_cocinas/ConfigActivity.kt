package com.escorial.pallet_cocinas

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ConfigActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FIRST_RUN = "first_run"
    }

    private val contraseñaCorrecta = "Aria9278"

    private val urlDefault = "http://0.0.0.0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        val layoutConfig = findViewById<LinearLayout>(R.id.layoutConfig)
        val layoutPassword = findViewById<LinearLayout>(R.id.layoutPassword)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnValidar = findViewById<Button>(R.id.btnValidar)

        val etApiUrl = findViewById<EditText>(R.id.etApiUrl)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)
        val btnCancelar = findViewById<AppCompatImageButton>(R.id.btnCancelar)
        val progressBar = findViewById<ProgressBar>(R.id.progressBarConfig)

        val prefs = getSharedPreferences("configuracion", Context.MODE_PRIVATE)

        val firstRun = intent.getBooleanExtra(EXTRA_FIRST_RUN, false)

        // Ocultar el layout de configuración hasta validar
        layoutConfig.visibility = View.GONE

        if (firstRun) {
            // Primer arranque: sin contraseña y sin poder salir hasta configurar.
            layoutPassword.visibility = View.GONE
            layoutConfig.visibility = View.VISIBLE
            btnCancelar.visibility = View.GONE
            val urlActual = prefs.getString("api_url", "") ?: ""
            etApiUrl.setText(if (urlActual == urlDefault) "" else urlActual)
        }

        btnValidar.setOnClickListener {
            if (etPassword.text.toString() == contraseñaCorrecta) {
                layoutPassword.visibility = View.GONE
                layoutConfig.visibility = View.VISIBLE
                etApiUrl.setText(prefs.getString("api_url", ""))
            } else {
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
            }
        }

        btnGuardar.setOnClickListener {
            val nuevaUrl = etApiUrl.text.toString().trim()
            if (nuevaUrl.isEmpty() ||
                !(nuevaUrl.startsWith("http://") || nuevaUrl.startsWith("https://"))
            ) {
                Toast.makeText(
                    this,
                    "Ingrese una URL válida (http:// o https://)",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnGuardar.isEnabled = false

            lifecycleScope.launch {
                val result = ApiClient.checkHealth(nuevaUrl)
                when {
                    result.healthy -> {
                        prefs.edit().putString("api_url", nuevaUrl).apply()
                        val versionMsg = result.version?.let { " (API v$it)" } ?: ""
                        Toast.makeText(
                            this@ConfigActivity,
                            "Configuración guardada$versionMsg",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    result.reachable -> {
                        // El servidor responde pero no está listo (p. ej. base de datos caída).
                        val versionMsg = result.version?.let { "API v$it " } ?: ""
                        Toast.makeText(
                            this@ConfigActivity,
                            "${versionMsg}accesible pero no está lista. Revise el servidor o la base de datos.",
                            Toast.LENGTH_LONG
                        ).show()
                        progressBar.visibility = View.GONE
                        btnGuardar.isEnabled = true
                    }
                    else -> {
                        Toast.makeText(
                            this@ConfigActivity,
                            "La URL de la API es incorrecta o el servidor no responde",
                            Toast.LENGTH_LONG
                        ).show()
                        progressBar.visibility = View.GONE
                        btnGuardar.isEnabled = true
                    }
                }
            }
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }

    override fun onBackPressed() {
        // En primer arranque el usuario no puede salir sin configurar la URL.
        if (intent.getBooleanExtra(EXTRA_FIRST_RUN, false)) {
            Toast.makeText(this, "Debe configurar la URL de la API", Toast.LENGTH_SHORT).show()
            return
        }
        super.onBackPressed()
    }
}
