package com.escorial.pallet_cocinas

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton

class ConfigActivity : AppCompatActivity() {

    private val contraseñaCorrecta = "Aria9278"

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

        val prefs = getSharedPreferences("configuracion", Context.MODE_PRIVATE)

        // Ocultar el layout de configuración hasta validar
        layoutConfig.visibility = View.GONE

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
            val nuevaUrl = etApiUrl.text.toString()
            prefs.edit().putString("api_url", nuevaUrl).apply()
            Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }
}
