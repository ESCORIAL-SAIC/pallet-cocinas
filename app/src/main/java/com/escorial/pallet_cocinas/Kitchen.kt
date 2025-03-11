package com.escorial.pallet_cocinas

data class Kitchen(
    val id: Int,
    val code: String,
    val description: String
) {
    companion object {
        fun getSampleKitchens(): List<Kitchen> {
            return listOf(
                Kitchen(1, "8205466297520", "Cocina Industrial A")
            )
        }
    }
}