package com.escorial.pallet_cocinas

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

object ApiClient {

    private const val DEFAULT_URL = "http://0.0.0.0"

    private var retrofit: Retrofit? = null

    fun getApiService(context: Context): ApiService {
        val prefs = context.getSharedPreferences("configuracion", Context.MODE_PRIVATE)
        val baseUrl = prefs.getString("api_url", DEFAULT_URL)!!

        if (retrofit == null || retrofit?.baseUrl().toString() != baseUrl) {
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        return retrofit!!.create(ApiService::class.java)
    }

    fun isConfigured(context: Context): Boolean {
        val prefs = context.getSharedPreferences("configuracion", Context.MODE_PRIVATE)
        val baseUrl = prefs.getString("api_url", DEFAULT_URL)?.trim()
        return !baseUrl.isNullOrEmpty() && baseUrl != DEFAULT_URL
    }

    suspend fun checkHealth(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
        // Retrofit descartable: no toca el singleton ni persiste nada.
        val service = try {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        } catch (e: IllegalArgumentException) {
            return@withContext false
        }

        try {
            service.getPallet("0")
            true
        } catch (h: HttpException) {
            // El servidor respondió (aunque con error HTTP): la URL es válida.
            true
        } catch (e: IOException) {
            false
        } catch (e: CancellationException) {
            // Re-lanzar para no romper structured concurrency si se cancela la corrutina.
            throw e
        } catch (e: Exception) {
            false
        }
    }
}