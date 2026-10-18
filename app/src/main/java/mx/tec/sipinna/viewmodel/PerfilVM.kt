/*
 * SIPINNA — PerfilVM.kt
 * Capa: ViewModel (estado y lógica de las pantallas)
 *
 * Lógica de "Mis datos": cargar y guardar la información de la cuenta (GET/PUT /account).
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 * Creado: 2026-10-17
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import mx.tec.sipinna.model.GENEROS
import mx.tec.sipinna.model.PerfilActualizar
import mx.tec.sipinna.model.PerfilCuenta
import mx.tec.sipinna.model.generoParaServidor
import mx.tec.sipinna.network.RetrofitClient
import mx.tec.sipinna.network.cookieDeSesion
import mx.tec.sipinna.network.mensajeDeExcepcion
import mx.tec.sipinna.network.mensajeDeRespuesta
import mx.tec.sipinna.view.DatosPerfil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Lo que muestra la pantalla "Mis datos" */
data class EstadoPerfil(
    val datos: DatosPerfil = DatosPerfil(),
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val error: String = "",
    val guardado: Boolean = false,
    val puedeCambiarCorreo: Boolean = true,   // false si la cuenta es de Google y no tiene contraseña
    val sesionVencida: Boolean = false        // el servidor respondió 401
)

/** Ver y editar los datos de la cuenta (GET /account y PUT /account) */
class PerfilVM : ViewModel() {

    private val _estado = MutableStateFlow(EstadoPerfil())
    /** Estado que observa la pantalla. */
    val estado: StateFlow<EstadoPerfil> = _estado

    /** Trae los datos del servidor al abrir la pantalla */
    fun cargar(token: String) {
        _estado.value = EstadoPerfil(cargando = true)
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.api.miCuenta(cookieDeSesion(token))
                val cuerpo = respuesta.body()
                when {
                    respuesta.code() == 401 -> _estado.value = EstadoPerfil(sesionVencida = true)
                    respuesta.isSuccessful && cuerpo != null -> _estado.value = EstadoPerfil(
                        datos = aPantalla(cuerpo),
                        puedeCambiarCorreo = cuerpo.tienePassword != false
                    )
                    else -> _estado.value = EstadoPerfil(
                        error = mensajeDeRespuesta(respuesta.code(), respuesta.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                _estado.value = EstadoPerfil(error = mensajeDeExcepcion(e))
            }
        }
    }

    /** Deja la pantalla vacía (al cerrar sesión o si la sesión caducó) */
    fun reiniciar() {
        _estado.value = EstadoPerfil()
    }

    /** La pantalla avisa cada vez que se escribe algo */
    fun cambiar(datos: DatosPerfil) {
        _estado.value = _estado.value.copy(datos = datos, error = "", guardado = false)
    }

    /** Guarda los cambios. Si salió bien, avisa el nombre nuevo (para el Perfil y la sesión). */
    fun guardar(token: String, alGuardar: (nombre: String) -> Unit) {
        val d = _estado.value.datos
        val problema = revisar(d)
        if (problema.isNotEmpty()) {
            _estado.value = _estado.value.copy(error = problema)
            return
        }

        val peticion = PerfilActualizar(
            nombre = d.nombre.trim(),
            email = d.correo.trim().lowercase().ifBlank { null },
            telefono = d.telefono.ifBlank { null }?.let { "+52$it" },
            edad = d.edad.toInt(),
            genero = generoParaServidor(d.genero).ifBlank { null }
        )

        _estado.value = _estado.value.copy(guardando = true, error = "", guardado = false)
        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.api.actualizarCuenta(cookieDeSesion(token), peticion)
                val cuerpo = respuesta.body()
                when {
                    respuesta.code() == 401 -> _estado.value = _estado.value.copy(guardando = false, sesionVencida = true)
                    respuesta.isSuccessful && cuerpo != null -> {
                        _estado.value = _estado.value.copy(
                            datos = aPantalla(cuerpo),
                            guardando = false,
                            guardado = true
                        )
                        alGuardar(cuerpo.nombre.orEmpty().ifBlank { peticion.nombre })
                    }
                    else -> _estado.value = _estado.value.copy(
                        guardando = false,
                        error = mensajeDeRespuesta(respuesta.code(), respuesta.errorBody()?.string())
                    )
                }
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(guardando = false, error = mensajeDeExcepcion(e))
            }
        }
    }

    /** Revisiones antes de mandar (las mismas reglas que el servidor) */
    private fun revisar(d: DatosPerfil): String {
        val edad = d.edad.toIntOrNull()
        return when {
            d.nombre.isBlank() -> "Escribe tu nombre."
            d.correo.isBlank() && d.telefono.isBlank() -> "Escribe tu correo, tu teléfono o ambos."
            d.correo.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(d.correo.trim()).matches() ->
                "Revisa tu correo."
            d.telefono.isNotBlank() && d.telefono.length != 10 -> "El teléfono debe tener 10 dígitos."
            edad == null || edad !in 1..120 -> "Escribe una edad válida."
            else -> ""
        }
    }

    /** Del formato del servidor al de la pantalla */
    private fun aPantalla(p: PerfilCuenta) = DatosPerfil(
        nombre = p.nombre.orEmpty(),
        correo = p.email.orEmpty(),
        telefono = sinLada(p.telefono.orEmpty()),
        edad = p.edad?.takeIf { it > 0 }?.toString().orEmpty(),
        genero = GENEROS.firstOrNull { it.first == p.genero }?.second.orEmpty()
    )

    /** "+525512345678" o "5512345678" → "5512345678" */
    private fun sinLada(telefono: String): String {
        val digitos = telefono.filter { it.isDigit() }
        return if (digitos.length > 10 && digitos.startsWith("52")) digitos.drop(2) else digitos
    }
}