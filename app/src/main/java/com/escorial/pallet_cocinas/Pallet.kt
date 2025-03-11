package com.escorial.pallet_cocinas

data class Pallet(
    val id: Int,
    val code: String,
    var products: ArrayList<Product>?
) {
    companion object {
        private val pallets = arrayListOf<Pallet>(
            Pallet(1, "1", null),
            Pallet(2, "2", null),
            Pallet(3, "3", null),
            Pallet(4, "4", null),
            Pallet(5, "5", null)
        )

        fun updateProductsInPallet(palletCode: String, productsToAdd: ArrayList<Product>) {
            pallets.find { it.code == palletCode }?.products = productsToAdd
        }

        fun getPallets(): ArrayList<Pallet> = pallets
    }
}