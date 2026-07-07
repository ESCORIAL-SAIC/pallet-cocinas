package com.escorial.pallet_cocinas.data.model

import java.util.UUID

data class Product(
    val serial: String,
    val productId: UUID,
    val productCode: String,
    val description: String,
    val type: String,
    val maxCantByPallet: Int,
    var isAvailable: Boolean,
    var deleted: Boolean
)
