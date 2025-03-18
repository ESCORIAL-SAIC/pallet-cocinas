package com.escorial.pallet_cocinas

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import androidx.core.view.isVisible

class TopBar(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private val usernameLabel: TextView
    private val completeNameLabel: TextView
    private val logoutButton: Button

    init {
        // Inflar el layout
        LayoutInflater.from(context).inflate(R.layout.top_bar, this, true)

        // Inicializar las vistas
        usernameLabel = findViewById(R.id.usernameLabel)
        completeNameLabel = findViewById(R.id.completeNameLabel)
        logoutButton = findViewById(R.id.logoutButton)

        // Aquí puedes agregar cualquier lógica de inicialización
    }

    // Método para actualizar los valores de las etiquetas
    fun setUserInfo(username: String?, completeName: String?) {
        usernameLabel.text = username ?: ""
        completeNameLabel.text = completeName ?: ""

        // Si los valores no son nulos, hacemos los TextViews visibles
        usernameLabel.isVisible = username != null
        completeNameLabel.isVisible = completeName != null
    }

    // Método para manejar la visibilidad del botón de logout
    fun setLogoutButtonVisibility(isVisible: Boolean) {
        logoutButton.isVisible = isVisible
    }

    // Método para manejar el evento de logout
    fun setOnLogoutClickListener(listener: OnClickListener) {
        logoutButton.setOnClickListener(listener)
    }
}
