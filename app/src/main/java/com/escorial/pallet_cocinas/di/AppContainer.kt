package com.escorial.pallet_cocinas.di

import android.content.Context
import com.escorial.pallet_cocinas.data.remote.ApiClient
import com.escorial.pallet_cocinas.data.remote.ApiService
import com.escorial.pallet_cocinas.data.repository.ConfigRepository
import com.escorial.pallet_cocinas.data.repository.ExpedicionRepository
import com.escorial.pallet_cocinas.data.repository.LoginRepository
import com.escorial.pallet_cocinas.data.repository.PalletRepository
import com.escorial.pallet_cocinas.data.repository.ProductRepository
import com.escorial.pallet_cocinas.data.repository.SessionRepository

class AppContainer(private val context: Context) {

    // No se cachea: preserva el comportamiento de ApiClient.getApiService(),
    // que reconstruye el Retrofit si "api_url" cambió desde ConfigActivity.
    private val apiService: ApiService
        get() = ApiClient.getApiService(context)

    val sessionRepository: SessionRepository by lazy { SessionRepository(context) }
    val configRepository: ConfigRepository by lazy { ConfigRepository(context) }

    val palletRepository: PalletRepository
        get() = PalletRepository(apiService)
    val loginRepository: LoginRepository
        get() = LoginRepository(apiService)
    val productRepository: ProductRepository
        get() = ProductRepository(apiService)
    val expedicionRepository: ExpedicionRepository
        get() = ExpedicionRepository(apiService, palletRepository)
}
