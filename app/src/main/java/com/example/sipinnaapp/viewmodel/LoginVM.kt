package com.example.sipinnaapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sipinnaapp.data.SessionManager
import com.example.sipinnaapp.model.LoginRequest
import com.example.sipinnaapp.network.RetrofitClient
import com.example.sipinnaapp.network.mensajeDeExcepcion
import com.example.sipinnaapp.network.mensajeDeRespuesta
import com.example.sipinnaapp.network.sacarTokenDeCookie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.sipinnaapp.model.ConfirmacionEliminar
import com.example.sipinnaapp.network.cookieDeSesion

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
                val respuesta = RetrofitClient.api.login(credenciales)

                if (respuesta.isSuccessful) {
                    val datos = respuesta.body()
                    // El token no viene en el cuerpo: viene en la cookie "session_token"
                    val token = sacarTokenDeCookie(respuesta)

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