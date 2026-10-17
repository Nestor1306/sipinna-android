package com.example.sipinnaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sipinnaapp.model.CambioContrasena
import com.example.sipinnaapp.model.SolicitudCodigo
import com.example.sipinnaapp.network.RetrofitClient
import com.example.sipinnaapp.network.mensajeDeExcepcion
import com.example.sipinnaapp.network.mensajeDeRespuesta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class EstadoRecuperar(
    val correo: String = "",
    val codigo: String = "",
    val nueva: String = "",
    val confirmar: String = "",
    val codigoEnviado: Boolean = false,   // false = pantalla del correo; true = código + contraseña nueva
    val cargando: Boolean = false,
    val aviso: String = "",               // "Te enviamos un código..." (lo dice el servidor)
    val error: String = "",
    val listo: Boolean = false            // la contraseña ya se cambió
)

// Recuperar la contraseña en 2 pasos: pedir el código al correo y cambiarla con ese código
class RecuperarVM : ViewModel() {

    companion object {
        const val DIGITOS_CODIGO = 6
    }

    private val _estado = MutableStateFlow(EstadoRecuperar())
    val estado: StateFlow<EstadoRecuperar> = _estado

    fun actualizarCorreo(valor: String) { _estado.value = _estado.value.copy(correo = valor) }
    fun actualizarCodigo(valor: String) {
        _estado.value = _estado.value.copy(codigo = valor.filter { it.isDigit() }.take(DIGITOS_CODIGO))
    }
    fun actualizarNueva(valor: String) { _estado.value = _estado.value.copy(nueva = valor) }
    fun actualizarConfirmar(valor: String) { _estado.value = _estado.value.copy(confirmar = valor) }

    // Lo que escribió (correo o teléfono) → (email, number) como los pide el backend.
    // Si no es válido, deja el error en pantalla y regresa null.
    private fun contacto(): Pair<String?, String?>? {
        val texto = _estado.value.correo.trim()
        if (texto.contains("@")) {
            if (!RegistroVM.correoValido(texto)) {
                _estado.value = _estado.value.copy(error = "El correo no es válido")
                return null
            }
            return texto to null
        }
        // Teléfono: 10 dígitos (si escribió +52 adelante, se quita)
        val digitos = texto.filter { it.isDigit() }.let {
            if (it.length == 12 && it.startsWith("52")) it.drop(2) else it
        }
        if (!RegistroVM.telefonoValido(digitos)) {
            _estado.value = _estado.value.copy(
                error = "Escribe tu correo o tu teléfono de ${RegistroVM.DIGITOS_TELEFONO} dígitos"
            )
            return null
        }
        return null to "+52$digitos"
    }

    // Paso 1: manda el código al correo o por SMS. También sirve para "Reenviar código".
    fun pedirCodigo() {
        val (email, telefono) = contacto() ?: return

        _estado.value = _estado.value.copy(cargando = true)
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.api.pedirCodigo(SolicitudCodigo(email = email, number = telefono))
                _estado.value = if (respuesta.isSuccessful) {
                    // El servidor responde lo mismo exista o no la cuenta (para no revelar datos)
                    _estado.value.copy(
                        cargando = false,
                        codigoEnviado = true,
                        aviso = respuesta.body()?.message
                            ?: "Si la cuenta existe, te enviamos un código de 6 dígitos."
                    )
                } else {
                    _estado.value.copy(
                        cargando = false,
                        error = mensajeDeRespuesta(respuesta.code(), respuesta.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(cargando = false, error = mensajeDeExcepcion(e))
            }
        }
    }

    // Paso 2: cambia la contraseña con el código
    fun cambiarContrasena() {
        val e = _estado.value
        val error = when {
            e.codigo.length != DIGITOS_CODIGO -> "El código tiene $DIGITOS_CODIGO dígitos"
            !contrasenaValida(e.nueva) -> errorDeContrasena(e.nueva) ?: "Revisa la contraseña nueva"
            e.nueva != e.confirmar -> "Las contraseñas no coinciden"
            else -> ""
        }
        if (error.isNotEmpty()) {
            _estado.value = e.copy(error = error)
            return
        }
        val (email, telefono) = contacto() ?: return

        _estado.value = e.copy(cargando = true)
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.api.cambiarContrasena(
                    CambioContrasena(email = email, number = telefono, code = e.codigo, new_password = e.nueva)
                )
                _estado.value = if (respuesta.isSuccessful) {
                    _estado.value.copy(cargando = false, listo = true)
                } else {
                    _estado.value.copy(
                        cargando = false,
                        error = mensajeDeRespuesta(respuesta.code(), respuesta.errorBody()?.string())
                    )
                }
            } catch (ex: Exception) {
                _estado.value = _estado.value.copy(cargando = false, error = mensajeDeExcepcion(ex))
            }
        }
    }

    // "Usar otro correo": regresa al paso 1
    fun cambiarCorreo() {
        _estado.value = _estado.value.copy(codigoEnviado = false, codigo = "", aviso = "")
    }

    fun cerrarError() { _estado.value = _estado.value.copy(error = "") }

    // Se llama al entrar a la pantalla, para empezar limpio
    fun reiniciar(correo: String = "") { _estado.value = EstadoRecuperar(correo = correo) }
}

