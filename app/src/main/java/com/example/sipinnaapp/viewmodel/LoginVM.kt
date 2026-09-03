package com.example.sipinnaapp.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

        // Por ahora simulamos login exitoso
        // Más adelante aquí llamaremos al API
        _estado.value = _estado.value.copy(error = "", loginExitoso = true)
    }

    fun cerrarError() {
        _estado.value = _estado.value.copy(error = "")
    }
}
