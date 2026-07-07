package com.escorial.pallet_cocinas.utils

import retrofit2.HttpException
import java.io.IOException

fun HttpException.apiMessage(): String {
    return this.response()?.errorBody()?.string()?.replace("\"", "") ?: "Error desconocido"
}

fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> "Error HTTP\n${apiMessage()}"
    is IOException -> "Error de conexión."
    else -> "Error al obtener datos."
}
