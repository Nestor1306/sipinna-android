/*
 * SIPINNA — EstadoLogin.kt
 * Capa: ViewModel (estado y lógica de las pantallas)
 *
 * Estado de la pantalla de Login y de la sesión activa.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 *   - Néstor Daniel Huidobro Aguilar
 * Creado: 2026-09-02
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.viewmodel

/**
 * Estado de la pantalla de Login y de la sesión activa.
 * @property token cookie session_token; "" si no hay sesión o es anónimo.
 * @property loginExitoso true cuando hay sesión con cuenta.
 * @property esAnonimo true si el usuario entró sin cuenta.
 */
data class EstadoLogin(
    val email: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val error: String = "",
    val loginExitoso: Boolean = false,
    val token: String = "",
    val nombreUsuario: String = "",
    val esAnonimo: Boolean = false
)