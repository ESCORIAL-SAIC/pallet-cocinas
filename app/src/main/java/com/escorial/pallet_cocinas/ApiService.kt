import com.escorial.pallet_cocinas.Pallet
import retrofit2.http.GET

interface ApiService {
    @GET("api/pallets") // Ruta de tu API
    suspend fun getPallets(): List<Pallet> // `suspend` para usar corutinas
}
