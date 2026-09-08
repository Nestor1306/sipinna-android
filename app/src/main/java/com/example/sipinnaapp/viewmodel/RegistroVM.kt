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

    // --- Actualizacion de campos ---

    fun actualizarNombre(valor: String) {
        _estado.value = _estado.value.copy(nombre = valor)
    }

    fun actualizarApellido(valor: String) {
        _estado.value = _estado.value.copy(apellido = valor)
    }

    fun actualizarGenero(valor: String) {
        _estado.value = _estado.value.copy(genero = valor)
    }

    fun actualizarEdad(valor: String) {
        _estado.value = _estado.value.copy(edad = valor)
    }

    fun actualizarTelefono(valor: String) {
        _estado.value = _estado.value.copy(telefono = valor)
    }

    fun actualizarEmail(valor: String) {
        _estado.value = _estado.value.copy(email = valor)
    }

    fun actualizarPassword(valor: String) {
        _estado.value = _estado.value.copy(password = valor)
    }

    fun actualizarConfirmarPassword(valor: String) {
        _estado.value = _estado.value.copy(confirmarPassword = valor)
    }

    // --- Registro ---

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
        if (email.isNotEmpty() && !email.contains("@")) {
            _estado.value = _estado.value.copy(error = "El email no es válido")
            return
        }
        if (password.length < 6) {
            _estado.value = _estado.value.copy(error = "La contraseña debe tener al menos 6 caracteres")
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
                    genero = _estado.value.genero,
                    email = email,
                    telefono = formatearTelefono(telefono),
                    password = password
                )

                val respuesta = RetrofitClient.api.registrarUsuario(usuario)

                if (respuesta.isSuccessful) {
                    _estado.value = _estado.value.copy(
                        cargando = false,
                        registroExitoso = true
                    )
                } else {
                    val detalle = respuesta.errorBody()?.string() ?: ""
                    _estado.value = _estado.value.copy(
                        cargando = false,
                        error = "Error ${respuesta.code()}: $detalle"
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

    // El backend exige formato e164: +521234567890 (sin espacios ni guiones)
    private fun formatearTelefono(telefono: String): String {
        if (telefono.isEmpty()) return ""

        val soloDigitos = telefono.filter { it.isDigit() }
        if (soloDigitos.isEmpty()) return ""

        return if (telefono.startsWith("+")) "+$soloDigitos" else "+52$soloDigitos"
    }

    fun cerrarError() {
        _estado.value = _estado.value.copy(error = "")
    }

    fun reiniciar() {
        _estado.value = EstadoRegistro()
    }
}
