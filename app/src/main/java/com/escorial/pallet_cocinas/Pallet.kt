package com.escorial.pallet_cocinas

import java.util.UUID

data class Pallet(
    val id: UUID,
    val codigo: String,
    val descripcion: String,
    val fecha_alta: String,
    var cenker_prod_x_pallet: ArrayList<Product>?
) {
    companion object {
        private val pallets = ArrayList<Pallet>()

        fun updateProductsInPallet(palletCode: String, productsToAdd: ArrayList<Product>) {
            pallets.find { it.codigo == palletCode }?.cenker_prod_x_pallet = productsToAdd
        }

        fun getPallets(): ArrayList<Pallet> = pallets
    }
}