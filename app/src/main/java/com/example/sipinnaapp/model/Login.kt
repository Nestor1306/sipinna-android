package com.example.sipinnaapp.model

// Lo que enviamos al servidor (POST /auth/login).
// Va el correo O el teléfono, nunca los dos: el que queda en null no se manda.
data class LoginRequest(
    val email: String? = null,
    val number: String? = null,   // teléfono con lada: "+525512345678"
    val password: String
)

// Lo que el servidor nos devuelve en el cuerpo de la respuesta.
// OJO: el token NO viene aquí. El backend lo manda en una cookie llamada
// "session_token" (ver network/Sesion.kt).
data class LoginResponse(
    val name: String?,
    val user_type: String?,   // "citizen", "administrador" o "alimentador"
    val zone_name: String?
)

// POST /auth/password/forgot → manda un código de 6 dígitos al correo
data class SolicitudCodigo(val email: String)

// POST /auth/password/reset → cambia la contraseña con ese código
data class CambioContrasena(
    val email: String,
    val code: String,
    val new_password: String
)

// Respuesta de los dos: {"message": "..."}
data class RespuestaMensaje(val message: String?)


// DELETE /account → el backend pide {"confirmation": "ELIMINAR"}
data class ConfirmacionEliminar(val confirmation: String = "ELIMINAR")