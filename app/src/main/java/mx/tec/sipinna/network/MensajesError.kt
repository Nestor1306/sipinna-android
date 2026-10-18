/*
 * SIPINNA — MensajesError.kt
 * Capa: Network (comunicación con el backend)
 *
 * Convierte errores de red y del servidor en mensajes claros para el usuario.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 *   - Néstor Daniel Huidobro Aguilar
 * Creado: 2026-09-22
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.network

import android.util.Log
import org.json.JSONObject
import java.io.IOException

// Mensajes de error amigables para el usuario.
// El detalle técnico no se muestra en pantalla; se manda a Logcat (etiqueta "SIPINNA")
// para que el equipo lo pueda revisar en Android Studio.

/** Cuando la llamada ni siquiera llegó al servidor (sin internet, servidor apagado, etc.) */
fun mensajeDeExcepcion(e: Exception): String {
    Log.e("SIPINNA", "Error en la llamada al servidor", e)

    return if (e is IOException) {
        "No hay conexión con el servidor. Revisa tu internet e inténtalo de nuevo."
    } else {
        "Ocurrió un error inesperado. Inténtalo de nuevo."
    }
}

/** Cuando el servidor sí respondió, pero con un código de error (400, 401, 500...) */
fun mensajeDeRespuesta(codigo: Int, cuerpo: String?): String {
    Log.e("SIPINNA", "El servidor respondió $codigo: $cuerpo")

    // El backend responde casi siempre {"error": "..."}, pero algunas rutas usan
    // "message" y el registro usa "erroruwu". Se toma el primero que venga.
    val mensajeServidor = try {
        val json = JSONObject(cuerpo ?: "")
        listOf("error", "message", "erroruwu")
            .firstNotNullOfOrNull { clave -> json.optString(clave).takeIf { it.isNotBlank() } }
            ?: ""
    } catch (_: Exception) {
        ""
    }

    // Solo lo mostramos si es un mensaje para personas (ej. "la cuenta no esta activa").
    // Los errores 500 y los de validación de Gin ("Key: '...' Error:Field...") son técnicos.
    val esTecnico = codigo >= 500 || mensajeServidor.contains("Key: '") || mensajeServidor.contains("json:")
    if (mensajeServidor.isNotBlank() && !esTecnico) {
        return mensajeServidor.replaceFirstChar { it.uppercase() }
    }

    return when (codigo) {
        401 -> "Tu sesión expiró. Vuelve a iniciar sesión."
        403 -> "No tienes permiso para hacer esto."
        404 -> "No se encontró lo que buscabas."
        409 -> "Ya existe una cuenta o un registro con esos datos."
        413 -> "El archivo es demasiado grande."
        429 -> "Espera un momento antes de intentarlo de nuevo."
        else -> if (codigo >= 500) {
            "El servidor tuvo un problema. Inténtalo más tarde."
        } else {
            "Ocurrió un error ($codigo). Inténtalo de nuevo."
        }
    }
}
