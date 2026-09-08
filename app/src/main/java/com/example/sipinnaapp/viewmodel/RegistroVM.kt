package com.example.sipinnaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.sipinnaapp.model.UsuarioRegistro
import com.example.sipinnaapp.network.RetrofitClient

class RegistroVM : ViewModel() {

    private val _estado = MutableStateFlow(EstadoRegistro())
    val estado: StateFlow<EstadoRegistro> = _estado

    fun actualizarNombre(valor: String) {
        _estado.value = _estado.value.copy(nombre = valor)
    }

    fun actualizarEmail(valor: String) {
        _estado.value = _estado.value.copy(email = valor)
    }

    fun actualizarPassword(valor: String) {
        _estado.value = _estado.value.copy(password = valor)
    }

    fun registrar() {
        val nombre = _estado.value.nombre.trim()
        val email = _estado.value.email.trim()
        val password = _estado.value.password

        // Validaciones locales primero
        if (nombre.isEmpty()) {
            _estado.value = _estado.value.copy(error = "El nombre es obligatorio")
            return
        }
        if (email.isEmpty() ) {
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

        // Llamada al servidor en background (no bloquea la pantalla)
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true)

            try {
                val usuario = UsuarioRegistro(nombre, email, password)
                val respuesta = RetrofitClient.api.registrarUsuario(usuario)

                if (respuesta.isSuccessful) {
                    _estado.value = _estado.value.copy(
                        cargando = false,
                        registroExitoso = true
                    )
                } else {
                    // Muestra el cuerpo del error que manda el servidor
                    val detalle = respuesta.errorBody()?.string() ?: ""
                    _estado.value = _estado.value.copy(
                        cargando = false,
                        error = "Error ${respuesta.code()}: $detalle"
                    )
                }
            } catch (e: Exception) {
                // Muestra el error real para poder diagnosticar
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

    fun reiniciar() {
        _estado.value = EstadoRegistro()
    }
}
