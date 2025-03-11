import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://192.168.20.162:5047" // Reemplaza con tu URL

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Convierte JSON a objetos
            .build()
            .create(ApiService::class.java)
    }
}
