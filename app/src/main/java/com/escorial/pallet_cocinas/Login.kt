package com.escorial.pallet_cocinas

import java.util.UUID

data class Login (
    var user: String,
    var password: String
)

data class LoginReponse (
    var id: UUID,
    var usuario_sistema: String,
    var nombre: String
)