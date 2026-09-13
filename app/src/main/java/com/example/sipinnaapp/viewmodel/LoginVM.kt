package com.example.sipinnaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.sipinnaapp.model.LoginRequest
import com.example.sipinnaapp.network.RetrofitClient

class LoginVM : ViewModel() {

    private val _estado = MutableStateFlow(EstadoLogin())
    val estado: StateFlow<EstadoLogin> = _estado

    fun actualizarEmail(valor: String) {
        _estado.value = _estado.value.copy(email = valor)
    }

    fun actualizarPassword(valor: String) {
        _estado.value = _estado.value.copy(password = valor)
    }

    fun login() {
        val email = _estado.value.email.trim()
        val password = _estado.value.password

        // Validaciones locales primero
        if (email.isEmpty()) {
            _estado.value = _estado.value.copy(error = "El email es obligatorio")
            return
        }
        if (!email.contains("@")) {
            _estado.value = _estado.value.copy(error = "El email no es válido")
            return
        }
        if (password.length < 6) {
            _estado.value = _estado.value.copy(error = "La contraseña debe tener al menos 6 caracteres")
            return
        }

        // Llamada al servidor en background
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true)

            try {
                val credenciales = LoginRequest(email, password)
                val respuesta = RetrofitClient.api.login(credenciales)

                if (respuesta.isSuccessful) {
                    val datos = respuesta.body()

                    if (datos != null) {
                        _estado.value = _estado.value.copy(
                            cargando = false,
                            loginExitoso = true,
                            token = datos.token,
                            nombreUsuario = datos.usuario.nombre
                        )
                    } else {
                        _estado.value = _estado.value.copy(
                            cargando = false,
                            error = "El servidor no devolvió datos"
                        )
                    }
                } else {
                    // 401 = credenciales incorrectas
                    _estado.value = _estado.value.copy(
                        cargando = false,
                        error = "Correo o contraseña incorrectos"
                    )
                }
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(
                    cargando = false,
                    error = "${e.javaClass.simpleName}: ${e.message}"
                )
            }
        }
    }

    fun cerrarError() {
        _estado.value = _estado.value.copy(error = "")
    }

    // Limpia todo y regresa al estado inicial
    fun cerrarSesion() {
        _estado.value = EstadoLogin()
    }
}
