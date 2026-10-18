/*
 * SIPINNA — LoginVM.kt
 * Capa: ViewModel (estado y lógica de las pantallas)
 *
 * Lógica de inicio de sesión, acceso anónimo, cierre de sesión y eliminación de cuenta.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 *   - Emiliano García Ramos
 *   - Néstor Daniel Huidobro Aguilar
 * Creado: 2026-09-02
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import mx.tec.sipinna.data.SessionManager
import mx.tec.sipinna.model.LoginRequest
import mx.tec.sipinna.network.RetrofitClient
import mx.tec.sipinna.network.mensajeDeExcepcion
import mx.tec.sipinna.network.mensajeDeRespuesta
import mx.tec.sipinna.network.sacarTokenDeCookie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.tec.sipinna.model.ConfirmacionEliminar
import mx.tec.sipinna.network.cookieDeSesion

/**
 * Inicio y cierre de sesión. Guarda la sesión con [SessionManager] (token cifrado)
 * para que el usuario siga dentro al volver a abrir la app.
 */
class LoginVM(
    application: Application
) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(EstadoLogin())
    val estado: StateFlow<EstadoLogin> = _estado

    private val sessionManager =
        SessionManager(application.applicationContext)

    // Al abrir la app revisa si ya había una sesión guardada.
    // Si la hay, el usuario entra directo al Home.
    init {
        val sesion = sessionManager.leerSesion()

        if (sesion.token.isNotBlank() || sesion.esAnonimo) {
            _estado.value = _estado.value.copy(
                loginExitoso = sesion.token.isNotBlank(),
                token = sesion.token,
                nombreUsuario = sesion.nombre,
                esAnonimo = sesion.esAnonimo
            )
        }
    }

    fun actualizarEmail(valor: String) {

        _estado.value =
            _estado.value.copy(
                email = valor
            )
    }

    fun actualizarPassword(valor: String) {

        _estado.value =
            _estado.value.copy(
                password = valor
            )
    }

    fun login() {
        // El mismo campo sirve para correo o teléfono
        val usuario = _estado.value.email.trim()
        val password = _estado.value.password

        val esCorreo = usuario.contains("@")
        // Si escribió "+52 55 1234 5678", nos quedamos con los 10 dígitos
        val digitos = usuario.filter { it.isDigit() }.let {
            if (it.length == 12 && it.startsWith("52")) it.drop(2) else it
        }

        val error = when {
            usuario.isEmpty() -> "Escribe tu correo o tu teléfono"
            esCorreo && !RegistroVM.correoValido(usuario) -> "El correo no es válido"
            !esCorreo && !RegistroVM.telefonoValido(digitos) ->
                "El teléfono debe tener ${RegistroVM.DIGITOS_TELEFONO} dígitos"
            // 6 y no 10: las cuentas que se crearon antes de las reglas nuevas siguen entrando
            password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
            else -> ""
        }
        if (error.isNotEmpty()) {
            _estado.value = _estado.value.copy(error = error)
            return
        }

        val credenciales = if (esCorreo) {
            LoginRequest(email = usuario, password = password)
        } else {
            LoginRequest(number = "+52$digitos", password = password)   // así se guardó al registrarse
        }

        _estado.value = _estado.value.copy(cargando = true)

        viewModelScope.launch {
            try {
                var respuesta = RetrofitClient.api.login(credenciales)

                // La app guarda el teléfono como +52XXXXXXXXXX, pero desde la web se puede
                // registrar solo con los 10 dígitos. Si no entra, se intenta de esa forma.
                if (respuesta.code() == 401 && !esCorreo) {
                    respuesta = RetrofitClient.api.login(
                        LoginRequest(number = digitos, password = password)
                    )
                }

                if (respuesta.isSuccessful) {
                    val datos = respuesta.body()
                    // El token no viene en el cuerpo: viene en la cookie "session_token"
                    val token = sacarTokenDeCookie(respuesta)
                    // La app es solo para ciudadanos: el personal (alimentador / administrador)
                    // trabaja desde la web. No se guarda su sesión.
                    val tipo = datos?.user_type
                    if (!tipo.isNullOrBlank() && tipo != "citizen") {
                        _estado.value = _estado.value.copy(
                            cargando = false,
                            error = "Esta cuenta es del personal de SIPINNA. Para administrar reportes usa la página web."
                        )
                        return@launch
                    }
                    if (datos != null && token.isNotBlank()) {
                        val nombre = datos.name ?: ""
                        sessionManager.guardarSesion(token = token, nombre = nombre, esAnonimo = false)
                        _estado.value = _estado.value.copy(
                            cargando = false,
                            loginExitoso = true,
                            token = token,
                            nombreUsuario = nombre,
                            esAnonimo = false
                        )
                    } else {
                        _estado.value = _estado.value.copy(
                            cargando = false,
                            error = "El servidor no devolvió datos"
                        )
                    }
                } else {
                    // 401 = el servidor no reconoció el correo/teléfono o la contraseña
                    val mensaje =
                        if (respuesta.code() == 401) "Correo, teléfono o contraseña incorrectos"
                        else mensajeDeRespuesta(respuesta.code(), respuesta.errorBody()?.string())
                    _estado.value = _estado.value.copy(cargando = false, error = mensaje)
                }
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(cargando = false, error = mensajeDeExcepcion(e))
            }
        }
    }

    fun cerrarError() {

        _estado.value =
            _estado.value.copy(
                error = ""
            )
    }

    fun entrarAnonimo() {
        // Actualizamos la interfaz inmediatamente
        _estado.value =
            EstadoLogin(
                nombreUsuario = "Anónimo",
                esAnonimo = true
            )

        // También guardamos la sesión anónima
        sessionManager.guardarSesion(
            token = "",
            nombre = "Anónimo",
            esAnonimo = true
        )
    }

    // Después de registrarse: el servidor ya dio la sesión, se guarda y se entra al Home
    fun entrarConSesion(token: String, nombre: String) {
        sessionManager.guardarSesion(token = token, nombre = nombre, esAnonimo = false)
        _estado.value = EstadoLogin(
            loginExitoso = true,
            token = token,
            nombreUsuario = nombre,
            esAnonimo = false
        )
    }

    // Después de editar "Mis datos": el nombre nuevo se ve en el Perfil y queda guardado
    fun actualizarNombre(nombre: String) {
        val token = _estado.value.token
        sessionManager.guardarSesion(token = token, nombre = nombre, esAnonimo = false)
        _estado.value = _estado.value.copy(nombreUsuario = nombre)
    }

    fun cerrarSesion() {

        // Limpia inmediatamente la interfaz
        _estado.value =
            EstadoLogin()

        // Borra la sesión guardada
        sessionManager.borrarSesion()
    }

    // El servidor dijo que la sesión ya no sirve: se borra y se avisa en el Login
    fun sesionExpirada() {
        cerrarSesion()
        _estado.value = _estado.value.copy(error = "Tu sesión expiró. Vuelve a iniciar sesión.")
    }

    // Elimina la cuenta en el servidor y, si sale bien, cierra la sesión
    fun eliminarCuenta(alTerminar: () -> Unit, alFallar: (String) -> Unit) {
        val token = _estado.value.token
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.api.eliminarCuenta(cookieDeSesion(token), ConfirmacionEliminar())
                if (respuesta.isSuccessful) {
                    cerrarSesion()
                    _estado.value = _estado.value.copy(error = "Tu cuenta se eliminó.")
                    alTerminar()
                } else {
                    alFallar(mensajeDeRespuesta(respuesta.code(), respuesta.errorBody()?.string()))
                }
            } catch (e: Exception) {
                alFallar(mensajeDeExcepcion(e))
            }
        }
    }
}