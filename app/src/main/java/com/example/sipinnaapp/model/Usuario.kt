package com.example.sipinnaapp.model

import com.google.gson.annotations.SerializedName

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


// GET /account y respuesta de PUT /account
// {"nombre": "...", "email": "..." o null, "telefono": "+52..." o null, "edad": 30,
//  "genero": "mujer", "user_type": "citizen", "zone_name": "", "has_password": true}
data class PerfilCuenta(
    val nombre: String?,
    val email: String?,
    val telefono: String?,
    val edad: Int?,
    val genero: String?,
    @SerializedName("has_password") val tienePassword: Boolean?
)

// PUT /account: lo que se manda al guardar. Lo que va en null no se manda.
// El teléfono va con +52 (el servidor pide formato internacional).
data class PerfilActualizar(
    val nombre: String,
    val email: String?,
    val telefono: String?,
    val edad: Int,
    val genero: String?
)