package com.example.sipinnaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sipinnaapp.model.ReporteRequest
import com.example.sipinnaapp.model.ReporteResumen
import com.example.sipinnaapp.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReporteVM : ViewModel() {

    private val _estado = MutableStateFlow(EstadoReporte())
    val estado: StateFlow<EstadoReporte> = _estado

    companion object {
        const val TOTAL_PASOS = 5
        const val MAX_FOTOS = 3

        // Opciones de la pantalla "Información del niño" (igual que en Figma)
        val OPCIONES_EDAD = listOf(
            "Menos de 5 años", "5 - 7 años", "8 - 10 años", "11 - 13 años", "14 - 17 años"
        )
        val OPCIONES_TRABAJO = listOf(
            "Vendedor ambulante", "Limpieza de calles", "Cargador / Cargadora",
            "Recolección de basura", "Agricultura / Campo", "Mendicidad", "Construcción", "Otro"
        )
        val OPCIONES_CONDICION = listOf(
            "Solo / Sola", "Con adultos", "Con otros niños", "Con familiares", "En situación de riesgo"
        )
    }

    // --- Paso 1: Locación ---

    fun actualizarDireccion(valor: String) {
        _estado.value = _estado.value.copy(direccion = valor)
    }

    fun actualizarCoordenadas(lat: Double, lng: Double) {
        _estado.value = _estado.value.copy(latitud = lat, longitud = lng, buscandoUbicacion = false)
    }

    fun actualizarBuscandoUbicacion(valor: Boolean) {
        _estado.value = _estado.value.copy(buscandoUbicacion = valor)
    }

    // --- Paso 2: Fotos ---

    fun agregarFotos(uris: List<String>) {
        val nuevas = (_estado.value.fotos + uris).distinct().take(MAX_FOTOS)
        _estado.value = _estado.value.copy(fotos = nuevas)
    }

    fun quitarFoto(uri: String) {
        _estado.value = _estado.value.copy(fotos = _estado.value.fotos - uri)
    }

    // --- Paso 3: Información del niño ---

    fun seleccionarEdad(valor: String) {
        _estado.value = _estado.value.copy(edadNino = valor)
    }

    // Se pueden elegir varios: si ya estaba, se quita; si no, se agrega
    fun alternarTipoTrabajo(valor: String) {
        val actuales = _estado.value.tiposTrabajo
        val nuevos = if (valor in actuales) actuales - valor else actuales + valor
        _estado.value = _estado.value.copy(tiposTrabajo = nuevos)
    }

    fun seleccionarCondicion(valor: String) {
        _estado.value = _estado.value.copy(condicion = valor)
    }

    // --- Paso 4: Descripción ---

    fun actualizarDescripcion(valor: String) {
        _estado.value = _estado.value.copy(descripcion = valor)
    }

    // --- Navegación entre pasos ---

    fun siguientePaso() {
        val e = _estado.value

        // Validación del paso en el que estamos antes de avanzar
        val errorPaso = when (e.pasoActual) {
            1 -> if (e.direccion.isBlank() && e.latitud == 0.0) "Indica la ubicación del reporte" else ""
            2 -> "" // las fotos son opcionales
            3 -> when {
                e.edadNino.isEmpty() -> "Selecciona la edad aproximada"
                e.tiposTrabajo.isEmpty() -> "Selecciona al menos un tipo de trabajo"
                e.condicion.isEmpty() -> "Selecciona en qué condición se encuentra"
                else -> ""
            }
            4 -> if (e.descripcion.trim().length < 10) "Describe lo que pasó (mínimo 10 caracteres)" else ""
            else -> ""
        }

        if (errorPaso.isNotEmpty()) {
            _estado.value = e.copy(error = errorPaso)
            return
        }

        if (e.pasoActual < TOTAL_PASOS) {
            _estado.value = e.copy(pasoActual = e.pasoActual + 1)
        }
    }

    fun pasoAnterior() {
        val actual = _estado.value.pasoActual
        if (actual > 1) {
            _estado.value = _estado.value.copy(pasoActual = actual - 1)
        }
    }

    fun irAPaso(paso: Int) {
        _estado.value = _estado.value.copy(pasoActual = paso.coerceIn(1, TOTAL_PASOS))
    }

    // --- Envío al servidor ---

    fun enviar(token: String) {
        val e = _estado.value

        viewModelScope.launch {
            _estado.value = e.copy(cargando = true)

            try {
                val ahora = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).format(Date())

                val peticion = ReporteRequest(
                    descripcion = e.descripcion.trim(),
                    latitud = e.latitud,
                    longitud = e.longitud,
                    direccion = e.direccion.trim(),
                    cantidad_ninos = 1,
                    edad_ninos = e.edadNino,
                    tipo_trabajo = e.tiposTrabajo.joinToString(", "),
                    horario_avistamiento = ahora,
                    condicion = e.condicion
                )

                val autorizacion =
                    if(token.isBlank()) null
                    else "Bearer $token"

                val respuesta =
                    RetrofitClient.api.crearReporte(autorizacion,peticion)

                if (respuesta.isSuccessful) {
                    val datos = respuesta.body()
                    val folio = datos?.folio ?: "SIN-FOLIO"

                    val resumen = ReporteResumen(
                        folio = folio,
                        estado = datos?.estado ?: "en_progreso",
                        descripcion = e.descripcion.trim(),
                        direccion = e.direccion.trim()
                    )

                    _estado.value = _estado.value.copy(
                        cargando = false,
                        folioGenerado = folio,
                        historial = listOf(resumen) + _estado.value.historial
                    )
                } else {
                    val detalle = respuesta.errorBody()?.string() ?: ""
                    _estado.value = _estado.value.copy(
                        cargando = false,
                        error = "Error ${respuesta.code()}: $detalle"
                    )
                }
            } catch (ex: Exception) {
                _estado.value = _estado.value.copy(
                    cargando = false,
                    error = "${ex.javaClass.simpleName}: ${ex.message}"
                )
            }
        }
    }

    fun cerrarError() {
        _estado.value = _estado.value.copy(error = "")
    }

    // Limpia el formulario pero conserva el historial de reportes enviados
    fun nuevoReporte() {
        _estado.value = EstadoReporte(historial = _estado.value.historial)
    }
}
