package com.example.sipinnaapp.data

import android.content.Context

// Datos de la sesión guardada en el teléfono
data class SesionUsuario(
    val token: String = "",
    val nombre: String = "",
    val esAnonimo: Boolean = false
)

// Guarda la sesión con SharedPreferences (un archivo pequeño dentro de la app).
// Así, al cerrar y volver a abrir la app, el usuario sigue con su sesión.
class SessionManager(context: Context) {

    private val preferencias =
        context.getSharedPreferences("sesion", Context.MODE_PRIVATE)

    fun leerSesion(): SesionUsuario {
        // Las versiones anteriores guardaban el token sin cifrar ("token").
        // Si existe, se cifra ahora y se borra el viejo.
        val tokenViejo = preferencias.getString("token", null)
        if (!tokenViejo.isNullOrEmpty()) {
            preferencias.edit()
                .putString("token_cifrado", Cifrado.cifrar(tokenViejo))
                .remove("token")
                .apply()
        }

        val token = Cifrado.descifrar(preferencias.getString("token_cifrado", "") ?: "") ?: ""

        return SesionUsuario(
            token = token,
            nombre = preferencias.getString("nombre", "") ?: "",
            esAnonimo = preferencias.getBoolean("es_anonimo", false)
        )
    }

    fun guardarSesion(token: String, nombre: String, esAnonimo: Boolean) {
        preferencias.edit()
            .putString("token_cifrado", Cifrado.cifrar(token))   // nunca en texto plano
            .putString("nombre", nombre)
            .putBoolean("es_anonimo", esAnonimo)
            .apply()
    }

    fun borrarSesion() {
        preferencias.edit().clear().apply()
    }
}
