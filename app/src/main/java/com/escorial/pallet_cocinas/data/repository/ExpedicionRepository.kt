package com.escorial.pallet_cocinas.data.repository

import com.escorial.pallet_cocinas.data.model.Pallet
import com.escorial.pallet_cocinas.data.remote.ApiService
import retrofit2.Response

class ExpedicionRepository(
    private val api: ApiService,
    private val palletRepository: PalletRepository
) {

    suspend fun buscarPallet(codigo: String): Pallet = palletRepository.getPalletWithProducts(codigo)

    suspend fun transferirPallets(pallets: List<Pallet>): Response<Unit> =
        api.postPalletTransfer(ArrayList(pallets))

    suspend fun desasociarPallets(pallets: List<Pallet>): Response<Unit> {
        var response: Response<Unit> = Response.success(Unit)
        pallets.forEach { pallet ->
            pallet.Products?.forEach { product -> product.deleted = true }
            response = api.postPalletProducts(pallet)
        }
        return response
    }
}
