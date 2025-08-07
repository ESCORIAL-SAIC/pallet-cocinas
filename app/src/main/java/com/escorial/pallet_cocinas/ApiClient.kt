package com.escorial.pallet_cocinas

import android.content.Context
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private var retrofit: Retrofit? = null

    fun getApiService(context: Context): ApiService {
        val prefs = context.getSharedPreferences("configuracion", Context.MODE_PRIVATE)
        val baseUrl = prefs.getString("api_url", "http://0.0.0.0")!!

        if (retrofit == null || retrofit?.baseUrl().toString() != baseUrl) {
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        return retrofit!!.create(ApiService::class.java)
    }
}