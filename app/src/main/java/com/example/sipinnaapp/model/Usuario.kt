package com.example.sipinnaapp.model

// Molde de los datos que el usuario llena para registrarse
// Es igual a como el backend espera recibirlos (POST /user)
data class UsuarioRegistro(
    val nombre: String,
    val email: String,
    val password: String
)
