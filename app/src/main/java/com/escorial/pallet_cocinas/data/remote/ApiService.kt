package com.escorial.pallet_cocinas.data.remote

import com.escorial.pallet_cocinas.data.model.Login
import com.escorial.pallet_cocinas.data.model.Pallet
import com.escorial.pallet_cocinas.data.model.Product
import com.escorial.pallet_cocinas.data.model.loginResponse
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
    suspend fun getProduct(@Query("numeroRecibido") numero: String, @Query("tipo") tipo: String): Product

    @POST("api/pallets/asociar-productos")
    suspend fun postPalletProducts(@Body pallet: Pallet): Response<Unit>

    @POST("api/login")
    suspend fun postLogin(@Body login: Login): Response<loginResponse>

    @POST("api/pallets/transferirExpedicion")
    suspend fun postPalletTransfer(@Body pallets: ArrayList<Pallet>): Response<Unit>
}
