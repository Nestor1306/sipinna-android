package com.example.sipinnaapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sipinnaapp.data.SessionManager
import com.example.sipinnaapp.model.LoginRequest
import com.example.sipinnaapp.network.RetrofitClient
import com.example.sipinnaapp.network.mensajeDeExcepcion
import com.example.sipinnaapp.network.mensajeDeRespuesta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

        val email =
            _estado.value.email.trim()

        val password =
            _estado.value.password

        if (email.isEmpty()) {

            _estado.value =
                _estado.value.copy(
                    error = "El email es obligatorio"
                )

            return
        }

        if (!email.contains("@")) {

            _estado.value =
                _estado.value.copy(
                    error = "El email no es válido"
                )

            return
        }

        if (password.length < 6) {

            _estado.value =
                _estado.value.copy(
                    error = "La contraseña debe tener al menos 6 caracteres"
                )

            return
        }

        _estado.value = _estado.value.copy(cargando = true)


        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true)

            try {

                val credenciales =
                    LoginRequest(
                        email,
                        password
                    )

                val respuesta =
                    RetrofitClient.api.login(
                        credenciales
                    )

                if (respuesta.isSuccessful) {

                    val datos =
                        respuesta.body()

                    if (datos != null) {
                        sessionManager.guardarSesion(
                            token = datos.token,
                            nombre = datos.user_name,
                            esAnonimo = false
                        )

                        _estado.value =
                            _estado.value.copy(
                                cargando = false,
                                loginExitoso = true,
                                token = datos.token,
                                nombreUsuario = datos.user_name,
                                esAnonimo = false
                            )
                    } else {

                        _estado.value =
                            _estado.value.copy(
                                cargando = false,
                                error = "El servidor no devolvió datos"
                            )
                    }

                } else {

                    // 401 = el servidor no reconoció el correo o la contraseña
                    val mensaje =
                        if (respuesta.code() == 401) "Correo o contraseña incorrectos"
                        else mensajeDeRespuesta(respuesta.code(), respuesta.errorBody()?.string())

                    _estado.value =
                        _estado.value.copy(
                            cargando = false,
                            error = mensaje
                        )
                }

            } catch (e: Exception) {

                _estado.value =
                    _estado.value.copy(
                        cargando = false,
                        error = mensajeDeExcepcion(e)
                    )
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
}