package com.escorial.pallet_cocinas.data.repository

import com.escorial.pallet_cocinas.data.model.Pallet
import com.escorial.pallet_cocinas.data.model.Product
import com.escorial.pallet_cocinas.data.remote.ApiService
import retrofit2.Response

class ProductRepository(private val api: ApiService) {

    suspend fun getProduct(serial: String, tipo: String): Product = api.getProduct(serial, tipo)

    suspend fun asociarProductos(pallet: Pallet): Response<Unit> = api.postPalletProducts(pallet)
}
