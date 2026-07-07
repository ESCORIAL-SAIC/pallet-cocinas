package com.escorial.pallet_cocinas.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SessionRepository(context: Context) {

    val preferences: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean = preferences.getBoolean("isLoggedIn", false)

    fun getUsername(default: String = "null"): String? = preferences.getString("username", default)

    fun getFullName(default: String = "null"): String? = preferences.getString("fullName", default)

    fun saveSession(username: String, fullName: String) {
        preferences.edit {
            putString("username", username)
            putString("fullName", fullName)
            putBoolean("isLoggedIn", true)
        }
    }

    fun logout() {
        preferences.edit { remove("isLoggedIn") }
    }
}
