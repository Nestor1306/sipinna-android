/*
 * SIPINNA — Login.kt
 * Capa: Model (estructuras de datos que viajan entre la app y el servidor)
 *
 * Peticiones y respuestas de autenticación: login, recuperación de contraseña y eliminación de cuenta.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 *   - Emiliano García Ramos
 *   - Néstor Daniel Huidobro Aguilar
 * Creado: 2026-09-13
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.model

/**
 * Lo que enviamos al servidor (POST /auth/login).
 * Va el correo O el teléfono, nunca los dos: el que queda en null no se manda.
 */
data class LoginRequest(
    val email: String? = null,
    val number: String? = null,   // teléfono con lada: "+525512345678"
    val password: String
)

/**
 * Lo que el servidor nos devuelve en el cuerpo de la respuesta.
 * OJO: el token NO viene aquí. El backend lo manda en una cookie llamada
 * "session_token" (ver network/Sesion.kt).
 */
data class LoginResponse(
    val name: String?,
    val user_type: String?,   // "citizen", "administrador" o "alimentador"
    val zone_name: String?
)

/**
 * POST /auth/password/forgot → manda un código de 6 dígitos al correo o por SMS.
 * Va el correo O el teléfono, nunca los dos: el que queda en null no se manda.
 */
data class SolicitudCodigo(
    val email: String? = null,
    val number: String? = null
)

/** POST /auth/password/reset → cambia la contraseña con ese código */
data class CambioContrasena(
    val email: String? = null,
    val number: String? = null,
    val code: String,
    val new_password: String
)

/** Respuesta de los dos: {"message": "..."} */
data class RespuestaMensaje(val message: String?)


/** DELETE /account → el backend pide {"confirmation": "ELIMINAR"} */
data class ConfirmacionEliminar(val confirmation: String = "ELIMINAR")