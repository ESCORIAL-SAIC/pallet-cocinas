package com.escorial.pallet_cocinas

class PalletRepository(private val api: ApiService) {

    suspend fun getPalletWithProducts(palletCode: String): Pallet {
        val pallet = api.getPallet(palletCode)
        pallet.Products = api.getPalletProducts(palletCode)
        return pallet
    }
}