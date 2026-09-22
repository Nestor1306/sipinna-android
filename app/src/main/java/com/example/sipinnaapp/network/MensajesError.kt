package com.example.sipinnaapp.network

import android.util.Log
import org.json.JSONObject
import java.io.IOException

// Mensajes de error amigables para el usuario.
// El detalle técnico no se muestra en pantalla; se manda a Logcat (etiqueta "SIPINNA")
// para que el equipo lo pueda revisar en Android Studio.

// Cuando la llamada ni siquiera llegó al servidor (sin internet, servidor apagado, etc.)
fun mensajeDeExcepcion(e: Exception): String {
    Log.e("SIPINNA", "Error en la llamada al servidor", e)

    return if (e is IOException) {
        "No hay conexión con el servidor. Revisa tu internet e inténtalo de nuevo."
    } else {
        "Ocurrió un error inesperado. Inténtalo de nuevo."
    }
}

// Cuando el servidor sí respondió, pero con un código de error (400, 401, 500...)
fun mensajeDeRespuesta(codigo: Int, cuerpo: String?): String {
    Log.e("SIPINNA", "El servidor respondió $codigo: $cuerpo")

    // El backend responde {"error": "mensaje"}; si viene, usamos ese mensaje
    val mensajeServidor = try {
        JSONObject(cuerpo ?: "").getString("error")
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
        400 -> "Algunos datos no son válidos. Revísalos e inténtalo de nuevo."
        401 -> "Tu sesión expiró. Cierra sesión y vuelve a entrar."
        403 -> "No tienes permiso para hacer esto."
        404 -> "No se encontró lo que buscabas."
        413 -> "El archivo es demasiado grande."
        else -> if (codigo >= 500) {
            "El servidor tuvo un problema. Inténtalo más tarde."
        } else {
            "Ocurrió un error ($codigo). Inténtalo de nuevo."
        }
    }
}
