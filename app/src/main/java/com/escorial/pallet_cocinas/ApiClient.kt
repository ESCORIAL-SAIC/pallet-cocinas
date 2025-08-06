package com.escorial.pallet_cocinas

import android.content.Context
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/*object ApiClient {
    private const val BASE_URL = "http://192.168.1.116:50003"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}*/
object ApiClient {

    private var retrofit: Retrofit? = null

    fun getApiService(context: Context): ApiService {
        val prefs = context.getSharedPreferences("configuracion", Context.MODE_PRIVATE)
        val baseUrl = prefs.getString("api_url", "http://192.168.1.116:50003")!!

        if (retrofit == null || retrofit?.baseUrl().toString() != baseUrl) {
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        return retrofit!!.create(ApiService::class.java)
    }
}