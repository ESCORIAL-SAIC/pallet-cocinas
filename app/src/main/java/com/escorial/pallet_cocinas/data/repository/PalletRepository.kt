package com.escorial.pallet_cocinas.data.repository

import com.escorial.pallet_cocinas.data.model.Pallet
import com.escorial.pallet_cocinas.data.remote.ApiService

class PalletRepository(private val api: ApiService) {

    suspend fun getPalletWithProducts(palletCode: String): Pallet {
        val pallet = api.getPallet(palletCode)
        pallet.Products = api.getPalletProducts(palletCode)
        return pallet
    }
}
