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
        LayoutInflater.from(context).inflate(R.layout.top_bar, this, true)

        usernameLabel = findViewById(R.id.usernameLabel)
        completeNameLabel = findViewById(R.id.completeNameLabel)
        logoutButton = findViewById(R.id.logoutButton)
    }

    fun setUserInfo(username: String?, completeName: String?) {
        usernameLabel.text = username ?: ""
        completeNameLabel.text = completeName ?: ""

        usernameLabel.isVisible = username != null
        completeNameLabel.isVisible = completeName != null
    }

    fun setLogoutButtonVisibility(isVisible: Boolean) {
        logoutButton.isVisible = isVisible
    }

    fun setOnLogoutClickListener(listener: OnClickListener) {
        logoutButton.setOnClickListener(listener)
    }
}
