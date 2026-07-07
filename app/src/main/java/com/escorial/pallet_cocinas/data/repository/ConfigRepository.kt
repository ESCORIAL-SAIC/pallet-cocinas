package com.escorial.pallet_cocinas.data.repository

import android.content.Context
import androidx.core.content.edit

class ConfigRepository(context: Context) {

    private val prefs = context.getSharedPreferences("configuracion", Context.MODE_PRIVATE)

    fun getApiUrl(): String = prefs.getString("api_url", "") ?: ""

    fun setApiUrl(url: String) {
        prefs.edit { putString("api_url", url) }
    }
}
