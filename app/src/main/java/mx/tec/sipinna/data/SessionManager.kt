/*
 * SIPINNA — SessionManager.kt
 * Capa: Data (almacenamiento local en el teléfono)
 *
 * Guarda y lee la sesión del usuario (token cifrado, nombre y si es anónimo).
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 *   - Néstor Daniel Huidobro Aguilar
 * Creado: 2026-09-22
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.data

import android.content.Context

/** Datos de la sesión guardada en el teléfono */
data class SesionUsuario(
    val token: String = "",
    val nombre: String = "",
    val esAnonimo: Boolean = false
)

/**
 * Guarda la sesión con SharedPreferences (un archivo pequeño dentro de la app).
 * Así, al cerrar y volver a abrir la app, el usuario sigue con su sesión.
 */
class SessionManager(context: Context) {

    private val preferencias =
        context.getSharedPreferences("sesion", Context.MODE_PRIVATE)

    /**
     * Lee la sesión guardada y descifra el token.
     * Si encuentra un token de una versión anterior guardado sin cifrar, lo cifra y borra el viejo.
     * @return la sesión; con token vacío si no hay sesión o no se pudo descifrar.
     */
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

    /**
     * Guarda la sesión. El token se cifra con [Cifrado] antes de escribirse.
     * @param token valor de la cookie session_token ("" para anónimo).
     * @param nombre nombre que se muestra en el Perfil.
     * @param esAnonimo true si entró sin cuenta.
     */
    fun guardarSesion(token: String, nombre: String, esAnonimo: Boolean) {
        preferencias.edit()
            .putString("token_cifrado", Cifrado.cifrar(token))   // nunca en texto plano
            .putString("nombre", nombre)
            .putBoolean("es_anonimo", esAnonimo)
            .apply()
    }

    /** Borra todos los datos de sesión (cerrar sesión o eliminar cuenta). */
    fun borrarSesion() {
        preferencias.edit().clear().apply()
    }
}
