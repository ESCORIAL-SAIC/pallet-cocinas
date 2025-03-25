package com.escorial.pallet_cocinas

import java.util.UUID

data class Pallet(
    val id: UUID,
    val codigo: String,
    val descripcion: String,
    val fecha_alta: String,
    var Products: ArrayList<Product>?,
    var Usuario: String
)