/*
 * SIPINNA — Sesion.kt
 * Capa: Network (comunicación con el backend)
 *
 * Manejo de la cookie session_token: extraerla de la respuesta y mandarla en cada petición.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 * Creado: 2026-10-05
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.network

import retrofit2.Response

/**
 * El backend guarda la sesión en una cookie llamada "session_token".
 * Al iniciar sesión la manda en el encabezado "Set-Cookie", por ejemplo:
 *   session_token=eyJhbGci...; Path=/; Max-Age=604800; HttpOnly; Secure
 * Nos quedamos solo con lo que va entre "session_token=" y el primer ";"
 */
fun sacarTokenDeCookie(respuesta: Response<*>): String {
    val cookie = respuesta.headers().values("Set-Cookie")
        .firstOrNull { it.startsWith("session_token=") }
        ?: return ""

    return cookie.substringAfter("session_token=").substringBefore(";")
}

/** Para las rutas que piden sesión, mandamos el token de regreso como cookie */
fun cookieDeSesion(token: String): String {
    return "session_token=$token"
}
