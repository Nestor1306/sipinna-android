package com.example.sipinnaapp.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.example.sipinnaapp.model.ReporteRequest
import com.example.sipinnaapp.network.RetrofitClient
import com.example.sipinnaapp.network.mensajeDeExcepcion
import com.example.sipinnaapp.network.mensajeDeRespuesta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Es AndroidViewModel porque necesita el "application" para leer las fotos de la galería
class ReporteVM(application: Application) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(EstadoReporte())
    val estado: StateFlow<EstadoReporte> = _estado

    companion object {
        const val TOTAL_PASOS = 5
        const val MAX_FOTOS = 2              // el diseño permite de 0 a 2 fotos
        const val LADO_MAXIMO_FOTO = 1600    // pixeles; las fotos se reducen antes de subirlas

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

                    // El reporte ya existe en la base de datos; ahora subimos sus fotos
                    val fotosFallidas = subirFotos(autorizacion, datos?.id ?: "", e.fotos)

                    _estado.value = _estado.value.copy(
                        cargando = false,
                        folioGenerado = datos?.folio ?: "SIN-FOLIO",
                        estadoGenerado = datos?.estado ?: "",
                        avisoFotos = if (fotosFallidas > 0) {
                            "Tu reporte se envió, pero $fotosFallidas foto(s) no se pudieron subir."
                        } else {
                            ""
                        }
                    )
                } else {
                    _estado.value = _estado.value.copy(
                        cargando = false,
                        error = mensajeDeRespuesta(respuesta.code(), respuesta.errorBody()?.string())
                    )
                }
            } catch (ex: Exception) {
                _estado.value = _estado.value.copy(
                    cargando = false,
                    error = mensajeDeExcepcion(ex)
                )
            }
        }
    }

    // --- Subida de fotos ---

    // Sube las fotos una por una al reporte recién creado.
    // Regresa cuántas fotos NO se pudieron subir (0 = todas bien).
    private suspend fun subirFotos(autorizacion: String?, reporteId: String, fotos: List<String>): Int {
        if (fotos.isEmpty()) return 0
        if (reporteId.isBlank()) return fotos.size   // sin id no sabemos a qué reporte subirlas

        var fallidas = 0

        for ((indice, uri) in fotos.withIndex()) {
            try {
                val bytes = prepararFoto(uri)
                if (bytes == null) {
                    fallidas++
                    continue
                }

                // Armamos el "formulario" con el archivo y su orden (1, 2)
                val archivo = MultipartBody.Part.createFormData(
                    "imagen",
                    "foto${indice + 1}.jpg",
                    bytes.toRequestBody("image/jpeg".toMediaType())
                )
                val orden = (indice + 1).toString().toRequestBody("text/plain".toMediaType())

                val respuesta = RetrofitClient.api.subirImagen(autorizacion, reporteId, archivo, orden)

                if (!respuesta.isSuccessful) {
                    Log.e("SIPINNA", "Foto ${indice + 1} rechazada: ${respuesta.code()} ${respuesta.errorBody()?.string()}")
                    fallidas++
                }
            } catch (ex: Exception) {
                Log.e("SIPINNA", "No se pudo subir la foto ${indice + 1}", ex)
                fallidas++
            }
        }

        return fallidas
    }

    // Lee la foto de la galería, la hace más pequeña y la convierte a JPEG.
    // Así se sube más rápido y el servidor siempre recibe el mismo formato.
    private suspend fun prepararFoto(uri: String): ByteArray? {
        val contexto = getApplication<Application>()

        // Coil (la misma librería que muestra las fotos en pantalla) la carga ya reducida
        // y además la endereza si estaba girada
        val peticion = ImageRequest.Builder(contexto)
            .data(uri)
            .size(LADO_MAXIMO_FOTO)
            .allowHardware(false)   // necesario para poder comprimirla después
            .build()

        val resultado = contexto.imageLoader.execute(peticion)
        val bitmap = (resultado.drawable as? BitmapDrawable)?.bitmap ?: return null

        // Comprimir es trabajo pesado, lo hacemos fuera del hilo de la pantalla
        return withContext(Dispatchers.IO) {
            val salida = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, salida)
            salida.toByteArray()
        }
    }

    // --- Historial desde la base de datos ---

    fun cargarHistorial(token: String) {
        // Los anónimos no tienen historial
        if (token.isBlank()) return

        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargandoHistorial = true, errorHistorial = "")

            try {
                val respuesta = RetrofitClient.api.misReportes("Bearer $token")

                if (respuesta.isSuccessful) {
                    _estado.value = _estado.value.copy(
                        cargandoHistorial = false,
                        historial = respuesta.body() ?: emptyList()
                    )
                } else {
                    _estado.value = _estado.value.copy(
                        cargandoHistorial = false,
                        errorHistorial = mensajeDeRespuesta(respuesta.code(), respuesta.errorBody()?.string())
                    )
                }
            } catch (ex: Exception) {
                _estado.value = _estado.value.copy(
                    cargandoHistorial = false,
                    errorHistorial = mensajeDeExcepcion(ex)
                )
            }
        }
    }

    // Al cerrar sesión se borra todo, para que otro usuario no vea estos reportes
    fun limpiarTodo() {
        _estado.value = EstadoReporte()
    }

    fun cerrarError() {
        _estado.value = _estado.value.copy(error = "")
    }

    // Limpia el formulario pero conserva el historial de reportes enviados.
    // (Al volver al Home, el historial se vuelve a pedir a la base de datos.)
    fun nuevoReporte() {
        _estado.value = EstadoReporte(historial = _estado.value.historial)
    }
}
