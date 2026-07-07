package com.escorial.pallet_cocinas

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.escorial.pallet_cocinas.databinding.TopBarBinding

class TopBar(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private val binding = TopBarBinding.inflate(LayoutInflater.from(context), this, true)

    fun setUserInfo(username: String?, completeName: String?) {
        binding.usernameLabel.text = username ?: ""
        binding.completeNameLabel.text = completeName ?: ""

        binding.usernameLabel.isVisible = username != null
        binding.completeNameLabel.isVisible = completeName != null
    }

    fun setLogoutButtonVisibility(isVisible: Boolean) {
        binding.logoutButton.isVisible = isVisible
    }

    fun setOnLogoutClickListener(listener: OnClickListener) {
        binding.logoutButton.setOnClickListener(listener)
    }
}
