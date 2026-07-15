package com.escorial.pallet_cocinas

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    /**
     * Chequea la disponibilidad de la API contra GET /health/ready.
     *
     * Distingue tres situaciones:
     * - no alcanzable (la URL no apunta a la API o el servidor no responde),
     * - alcanzable pero no lista (p. ej. base de datos caída → 503),
     * - sana (200 + "Healthy").
     * De paso obtiene la versión que reporta el endpoint.
     */
    suspend fun checkHealth(baseUrl: String): HealthCheckResult = withContext(Dispatchers.IO) {
        // Retrofit descartable: no toca el singleton ni persiste nada.
        val service = try {
            buildService(baseUrl)
        } catch (e: IllegalArgumentException) {
            return@withContext HealthCheckResult(reachable = false, healthy = false, version = null)
        }

        try {
            val response = service.getHealthReady()
            // En 503 el cuerpo válido viaja en errorBody(); lo parseamos igual
            // para poder mostrar la versión aunque la API no esté lista.
            val body = response.body() ?: parseHealthBody(response.errorBody()?.string())
            val healthy = response.isSuccessful &&
                body?.status.equals("Healthy", ignoreCase = true)
            HealthCheckResult(reachable = true, healthy = healthy, version = body?.version)
        } catch (e: IOException) {
            // No se pudo establecer conexión: la URL no apunta a la API.
            HealthCheckResult(reachable = false, healthy = false, version = null)
        } catch (e: CancellationException) {
            // Re-lanzar para no romper structured concurrency si se cancela la corrutina.
            throw e
        } catch (e: Exception) {
            // El servidor respondió algo inesperado: alcanzable, pero no sano.
            HealthCheckResult(reachable = true, healthy = false, version = null)
        }
    }

    /** Versión reportada por GET /version, o null si no se pudo obtener. */
    suspend fun getApiVersion(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            getApiService(context).getVersion().version
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun buildService(baseUrl: String): ApiService =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

    private fun parseHealthBody(json: String?): HealthResponse? {
        if (json.isNullOrBlank()) return null
        return try {
            Gson().fromJson(json, HealthResponse::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }
}
