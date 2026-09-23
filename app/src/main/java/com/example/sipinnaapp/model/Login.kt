package com.example.sipinnaapp.model

// Lo que enviamos al servidor
data class LoginRequest(
    val email: String,
    val password: String
)

// Lo que el servidor nos devuelve
data class LoginResponse(
    val token: String,
    val user_name: String
)

// El usuario que viene anidado dentro de la respuesta
data class UsuarioLogeado(
    val id: String,
    val nombre: String,
    val email: String?,
    val rol: String
)
