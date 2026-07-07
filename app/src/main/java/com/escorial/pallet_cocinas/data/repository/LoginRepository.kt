package com.escorial.pallet_cocinas.data.repository

import com.escorial.pallet_cocinas.data.model.Login
import com.escorial.pallet_cocinas.data.model.loginResponse
import com.escorial.pallet_cocinas.data.remote.ApiService

class LoginRepository(private val api: ApiService) {

    suspend fun login(username: String, password: String): loginResponse? {
        val response = api.postLogin(Login(username, password))
        return if (response.isSuccessful) response.body() else null
    }
}
