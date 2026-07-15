package com.escorial.pallet_cocinas

/** Respuesta de GET /health/ready */
data class HealthResponse(
    val status: String?,
    val version: String?,
    val checks: List<HealthCheck>?
)

data class HealthCheck(
    val name: String?,
    val status: String?,
    val description: String?
)

/**
 * Resultado del chequeo de disponibilidad de la API.
 *
 * - [reachable]: el servidor respondió algo (la URL apunta a la API).
 * - [healthy]: /health/ready devolvió 200 con estado "Healthy".
 * - [version]: versión reportada por la API, si estuvo disponible.
 */
data class HealthCheckResult(
    val reachable: Boolean,
    val healthy: Boolean,
    val version: String?
)
