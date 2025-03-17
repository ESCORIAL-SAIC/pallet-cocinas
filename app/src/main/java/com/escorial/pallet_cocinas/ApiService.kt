import com.escorial.pallet_cocinas.Pallet
import com.escorial.pallet_cocinas.Product
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.Response

interface ApiService {
    @GET("api/pallets")
    suspend fun getPallet(@Query("numero") numero: String): Pallet

    @GET("api/cocinas")
    suspend fun getKitchen(@Query("numero") numero: Int): Product

    @GET("api/termos")
    suspend fun getHeater(@Query("numero") numero: Int): Product

    @GET("api/pallets/productos")
    suspend fun getPalletProducts(@Query("numero") numero: String): ArrayList<Product>?

    @GET("api/productos")
    suspend fun getProduct(@Query("numero") numero: Int, @Query("tipo") tipo: String): Product

    @POST("api/pallets/asociar-productos")
    suspend fun postPalletProducts(@Body pallet: Pallet): Response<Unit>
}
