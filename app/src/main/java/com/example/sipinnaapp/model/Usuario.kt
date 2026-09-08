package com.example.sipinnaapp.model

// Molde de los datos que el usuario llena para registrarse
// Es igual a como el backend espera recibirlos (POST /user)
data class UsuarioRegistro(
    val nombre: String,
    val edad: Int,
    val genero: String,
    val email: String,
    val telefono: String,
    val password: String
)
