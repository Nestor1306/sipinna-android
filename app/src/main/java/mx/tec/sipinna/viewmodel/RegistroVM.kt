/*
 * SIPINNA — RegistroVM.kt
 * Capa: ViewModel (estado y lógica de las pantallas)
 *
 * Lógica y validaciones del registro de ciudadanos.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 *   - Néstor Daniel Huidobro Aguilar
 * Creado: 2026-09-02
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.tec.sipinna.model.UsuarioRegistro
import mx.tec.sipinna.network.RetrofitClient
import mx.tec.sipinna.network.mensajeDeExcepcion
import mx.tec.sipinna.network.mensajeDeRespuesta
import mx.tec.sipinna.network.sacarTokenDeCookie
import mx.tec.sipinna.model.generoParaServidor

/** Registro de ciudadanos (POST /auth/citizen). Valida los campos antes de enviarlos. */
class RegistroVM : ViewModel() {
    companion object {
        const val MAX_DIGITOS_EDAD = 2
        const val DIGITOS_TELEFONO = 10   // teléfono de México sin lada internacional

        private val FORMATO_CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        /** @return true si [correo] tiene formato nombre@dominio.ext. */
        fun correoValido(correo: String): Boolean = FORMATO_CORREO.matches(correo.trim())

        /** @return true si [telefono] tiene 10 dígitos (México sin lada internacional). */
        fun telefonoValido(telefono: String): Boolean = telefono.length == DIGITOS_TELEFONO
    }

    private val _estado = MutableStateFlow(EstadoRegistro())
    /** Estado que observa la pantalla. */
    val estado: StateFlow<EstadoRegistro> = _estado

    // --- Actualizacion de campos ---

    /** Guarda el nombre escrito. */
    fun actualizarNombre(valor: String) {
        _estado.value = _estado.value.copy(nombre = valor)
    }

    /** Guarda el apellido escrito. */
    fun actualizarApellido(valor: String) {
        _estado.value = _estado.value.copy(apellido = valor)
    }

    /** Guarda el género elegido (texto de la lista). */
    fun actualizarGenero(valor: String) {
        _estado.value = _estado.value.copy(genero = valor)
    }

    /** Edad y teléfono solo aceptan números: cualquier otro carácter se descarta al escribir */
    fun actualizarEdad(valor: String) {
        _estado.value = _estado.value.copy(edad = valor.filter { it.isDigit() }.take(MAX_DIGITOS_EDAD))
    }

    /** Guarda el teléfono; solo dígitos y máximo [DIGITOS_TELEFONO]. */
    fun actualizarTelefono(valor: String) {
        _estado.value = _estado.value.copy(telefono = valor.filter { it.isDigit() }.take(DIGITOS_TELEFONO))
    }

    /** Guarda el correo escrito. */
    fun actualizarEmail(valor: String) {
        _estado.value = _estado.value.copy(email = valor)
    }

    /** Guarda la contraseña escrita. */
    fun actualizarPassword(valor: String) {
        _estado.value = _estado.value.copy(password = valor)
    }

    /** Guarda la confirmación de la contraseña. */
    fun actualizarConfirmarPassword(valor: String) {
        _estado.value = _estado.value.copy(confirmarPassword = valor)
    }

    // --- Registro ---

    /**
     * Valida los datos y crea la cuenta. Si el servidor responde bien, deja
     * registroExitoso = true y el token de sesión en el estado.
     */
    fun registrar() {
        val nombre = _estado.value.nombre.trim()
        val apellido = _estado.value.apellido.trim()
        val email = _estado.value.email.trim()
        val telefono = _estado.value.telefono.trim()
        val password = _estado.value.password

        // Validaciones locales primero
        if (nombre.isEmpty()) {
            _estado.value = _estado.value.copy(error = "El nombre es obligatorio")
            return
        }
        if (email.isEmpty() && telefono.isEmpty()) {
            _estado.value = _estado.value.copy(error = "Ingresa tu correo o tu teléfono")
            return
        }
        if (email.isNotEmpty() && !correoValido(email)) {
            _estado.value = _estado.value.copy(error = "El correo no es válido (ejemplo: tucorreo@ejemplo.com)")
            return
        }
        if (telefono.isNotEmpty() && !telefonoValido(telefono)) {
            _estado.value = _estado.value.copy(error = "El teléfono debe tener $DIGITOS_TELEFONO dígitos")
            return
        }
        // Reglas de la contraseña (ver ReglasContrasena.kt)
        val errorContrasena = errorDeContrasena(password)
        if (errorContrasena != null) {
            _estado.value = _estado.value.copy(error = errorContrasena)
            return
        }

        if (password != _estado.value.confirmarPassword) {
            _estado.value = _estado.value.copy(error = "Las contraseñas no coinciden")
            return
        }

        // Llamada al servidor en background (no bloquea la pantalla)
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true)

            try {
                val usuario = UsuarioRegistro(
                    nombre = "$nombre $apellido".trim(),
                    edad = _estado.value.edad.toIntOrNull() ?: 0,
                    genero = generoParaServidor(_estado.value.genero),
                    email = email,
                    telefono = formatearTelefono(telefono),
                    password = password
                )

                val respuesta = RetrofitClient.api.registrarUsuario(usuario)

                if (respuesta.isSuccessful) {
                    _estado.value = _estado.value.copy(
                        cargando = false,
                        registroExitoso = true,
                        // El registro ya deja la sesión iniciada (cookie "session_token")
                        tokenSesion = sacarTokenDeCookie(respuesta)
                    )
                } else {
                    // Si el backend manda {"error": "mensaje"} se muestra ese mensaje
                    _estado.value = _estado.value.copy(
                        cargando = false,
                        error = mensajeDeRespuesta(respuesta.code(), respuesta.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(
                    cargando = false,
                    error = mensajeDeExcepcion(e)
                )
            }
        }
    }

    /** El backend exige formato e164: +521234567890 (sin espacios ni guiones) */
    private fun formatearTelefono(telefono: String): String {
        if (telefono.isEmpty()) return ""

        val soloDigitos = telefono.filter { it.isDigit() }
        if (soloDigitos.isEmpty()) return ""

        return if (telefono.startsWith("+")) "+$soloDigitos" else "+52$soloDigitos"
    }

    /** Oculta el mensaje de error. */
    fun cerrarError() {
        _estado.value = _estado.value.copy(error = "")
    }

    /** Limpia el formulario. */
    fun reiniciar() {
        _estado.value = EstadoRegistro()
    }
}
