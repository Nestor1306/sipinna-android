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


// Género: lo que guarda el servidor (izquierda) y cómo se ve en la app (derecha).
// La web y PUT /account usan los valores de la izquierda.
val GENEROS = listOf(
    "hombre" to "Masculino",
    "mujer" to "Femenino",
    "otro" to "Otro",
    "prefiero-no-decir" to "Prefiero no mencionarlo"
)

// "Masculino" → "hombre" (para mandarlo al servidor)
fun generoParaServidor(etiqueta: String): String =
    GENEROS.firstOrNull { it.second == etiqueta }?.first ?: ""
