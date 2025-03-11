package com.escorial.pallet_cocinas

data class Product(
    val numero: String,
    val idproducto: String,
    val maxCantByPallet: Int,
    var ingreso_stock: Boolean
) {
    companion object {

        private val kitchens = ArrayList<Product>()

        private val heaters = ArrayList<Product>()

        fun getKitchens(): ArrayList<Product> = kitchens
        fun getHeaters(): ArrayList<Product> = heaters

    }
}
