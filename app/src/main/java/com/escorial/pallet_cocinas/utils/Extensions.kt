package com.escorial.pallet_cocinas.utils

import retrofit2.HttpException

fun HttpException.apiMessage(): String {
    return this.response()?.errorBody()?.string()?.replace("\"", "") ?: "Error desconocido"
}
