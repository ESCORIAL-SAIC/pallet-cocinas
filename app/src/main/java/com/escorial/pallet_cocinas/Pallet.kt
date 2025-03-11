package com.escorial.pallet_cocinas

data class Pallet(
    val id: Int,
    val code: String,
    val kitchenId: Int
) {
    companion object {
        fun getSamplePallets(): List<Pallet> {
            return listOf(
                Pallet(1, "8126116731721", 1)
            )
        }
    }
}