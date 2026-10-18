/*
 * SIPINNA — EstadoRegistro.kt
 * Capa: ViewModel (estado y lógica de las pantallas)
 *
 * Estado de la pantalla de Registro.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 *   - Néstor Daniel Huidobro Aguilar
 * Creado: 2026-09-02
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.viewmodel



/** Lo que el usuario escribe en el registro y el estado de la pantalla. */
data class EstadoRegistro(
    val nombre: String = "",
    val apellido: String = "",
    val genero: String = "",
    val edad: String = "",
    val telefono: String = "",
    val email: String = "",
    val password: String = "",
    val confirmarPassword:String = "",

    //control de pantalla

    val cargando: Boolean = false,   // true mientras espera respuesta del servidor
    val error: String = "",
    val registroExitoso: Boolean = false,
    val tokenSesion: String = ""     // el servidor inicia sesión al registrarse; "" si no llegó
)
