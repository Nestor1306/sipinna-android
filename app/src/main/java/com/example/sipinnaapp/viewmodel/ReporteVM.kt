package com.example.sipinnaapp.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.example.sipinnaapp.model.ImagenPorSubir
import com.example.sipinnaapp.model.RegistroImagenesRequest
import com.example.sipinnaapp.model.ReporteRequest
import com.example.sipinnaapp.network.RetrofitClient
import com.example.sipinnaapp.network.cookieDeSesion
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
        private const val REINTENTAR = " Tu avance se guardó: toca \"Enviar reporte\" para reintentar."

        const val TOTAL_PASOS = 5
        const val MAX_FOTOS = 2              // el diseño permite hasta 2 fotos

        // Por ahora el backend solo acepta enviar (PUT /report/{id}/submit) reportes
        // con al menos una foto subida, así que la pedimos. Si el backend cambia para
        // permitir 0 fotos, basta con poner esto en false.
        const val FOTO_OBLIGATORIA = false
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
            2 -> if (FOTO_OBLIGATORIA && e.fotos.isEmpty()) "Agrega al menos una foto del lugar" else ""
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
            _estado.value = sinProgresoDeEnvio(_estado.value).copy(pasoActual = actual - 1)
        }
    }

    fun irAPaso(paso: Int) {
        _estado.value = sinProgresoDeEnvio(_estado.value).copy(pasoActual = paso.coerceIn(1, TOTAL_PASOS))
    }

    // Si el usuario regresa a editar después de un envío que falló a la mitad, el borrador
    // que ya existe en el servidor tiene los datos viejos. Lo olvidamos para que el
    // siguiente "Enviar" cree uno nuevo con los datos corregidos. (El borrador viejo se
    // queda en DRAFT: no aparece en el historial ni lo analiza Jev.)
    private fun sinProgresoDeEnvio(e: EstadoReporte): EstadoReporte {
        if (e.reporteIdBorrador.isBlank()) return e
        return e.copy(reporteIdBorrador = "", idsImagenes = emptyList(), fotosSubidas = emptySet())
    }

    // --- Envío al servidor ---
    //
    // El backend crea un reporte en 4 pasos:
    //   1. POST /report                          → crea el reporte en DRAFT, regresa su id
    //   2. POST /report/{id}/images              → registra cuántas fotos vienen, regresa sus ids
    //   3. PUT  /report/{id}/images/{image_id}   → sube cada foto a S3 (una por llamada)
    //   4. PUT  /report/{id}/submit              → el reporte pasa de DRAFT a "registrado"
    //
    // Cada paso que termina bien se guarda en el estado. Si algo falla, el usuario vuelve
    // a tocar "Enviar reporte" y se continúa desde el paso que falló, sin duplicar nada.

    fun enviar(token: String) {
        val e = _estado.value
        if (e.cargando) return   // evita enviar dos veces con doble toque

        // Las 4 rutas piden sesión en el backend
        if (token.isBlank()) {
            _estado.value = e.copy(
                error = "Por ahora no se pueden enviar reportes sin cuenta. Crea una cuenta o inicia sesión para enviarlo."
            )
            return
        }
        val sesion = cookieDeSesion(token)

        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true, error = "", progresoEnvio = "Creando reporte…")

            try {
                // ---- Paso 1: crear el borrador ----
                var reporteId = _estado.value.reporteIdBorrador
                if (reporteId.isBlank()) {
                    val respuesta = RetrofitClient.api.crearReporte(sesion, armarPeticion(e))
                    if (!respuesta.isSuccessful) {
                        return@launch falloEnPaso(1, respuesta.code(), respuesta.errorBody()?.string())
                    }
                    reporteId = respuesta.body()?.reporteId.orEmpty()
                    if (reporteId.isBlank()) {
                        return@launch falloEnPaso(1, null, "El servidor no regresó el id del reporte")
                    }
                    _estado.value = _estado.value.copy(reporteIdBorrador = reporteId)
                }

                // ---- Paso 2: registrar las fotos ----
                var ids = _estado.value.idsImagenes
                if (e.fotos.isNotEmpty() && ids.isEmpty()) {
                    _estado.value = _estado.value.copy(progresoEnvio = "Preparando fotos…")

                    val peticion = RegistroImagenesRequest(
                        e.fotos.indices.map { i -> ImagenPorSubir("foto${i + 1}.jpg", "image/jpeg") }
                    )
                    val respuesta = RetrofitClient.api.registrarImagenes(sesion, reporteId, peticion)
                    if (!respuesta.isSuccessful) {
                        return@launch falloEnPaso(2, respuesta.code(), respuesta.errorBody()?.string())
                    }
                    ids = respuesta.body()?.ids.orEmpty()
                    if (ids.size != e.fotos.size) {
                        return@launch falloEnPaso(2, null, "Se esperaban ${e.fotos.size} ids de fotos y llegaron ${ids.size}")
                    }
                    _estado.value = _estado.value.copy(idsImagenes = ids)
                }

                // ---- Paso 3: subir cada foto ----
                for ((indice, uri) in e.fotos.withIndex()) {
                    val imagenId = ids[indice]
                    if (imagenId in _estado.value.fotosSubidas) continue   // ya subió en un intento anterior

                    _estado.value = _estado.value.copy(
                        progresoEnvio = "Subiendo foto ${indice + 1} de ${e.fotos.size}…"
                    )

                    val bytes = prepararFoto(uri)
                        ?: return@launch falloEnPaso(3, null, "No se pudo leer la foto ${indice + 1}")

                    val archivo = MultipartBody.Part.createFormData(
                        "file",
                        "foto${indice + 1}.jpg",
                        bytes.toRequestBody("image/jpeg".toMediaType())
                    )
                    val respuesta = RetrofitClient.api.subirImagen(sesion, reporteId, imagenId, archivo)
                    if (!respuesta.isSuccessful || respuesta.body()?.success == false) {
                        return@launch falloEnPaso(
                            3,
                            respuesta.code(),
                            respuesta.errorBody()?.string() ?: respuesta.body()?.message
                        )
                    }
                    _estado.value = _estado.value.copy(fotosSubidas = _estado.value.fotosSubidas + imagenId)
                }

                // ---- Paso 4: enviar (DRAFT → registrado) ----
                _estado.value = _estado.value.copy(progresoEnvio = "Enviando reporte…")
                val respuesta = RetrofitClient.api.enviarReporte(sesion, reporteId)
                if (!respuesta.isSuccessful) {
                    return@launch falloEnPaso(4, respuesta.code(), respuesta.errorBody()?.string())
                }

                // El backend no regresa el folio al crear el reporte, así que lo buscamos
                // en el historial del usuario: es el enviado más reciente.
                val folio = folioMasReciente(sesion)

                _estado.value = _estado.value.copy(
                    cargando = false,
                    progresoEnvio = "",
                    folioGenerado = folio.ifBlank { "Consúltalo en tu historial" },
                    estadoGenerado = "registrado",   // todo reporte enviado empieza así
                    avisoFotos = ""
                )
            } catch (ex: Exception) {
                _estado.value = _estado.value.copy(
                    cargando = false,
                    progresoEnvio = "",
                    error = mensajeDeExcepcion(ex) + REINTENTAR
                )
            }
        }
    }

    // Arma el JSON del paso 1. La dirección escrita y la condición no tienen columna en la
    // base, así que van al final de la descripción para no perderlas.
    private fun armarPeticion(e: EstadoReporte): ReporteRequest {
        val partes = mutableListOf(e.descripcion.trim())
        if (e.direccion.isNotBlank()) partes += "Dirección: ${e.direccion.trim()}"
        if (e.condicion.isNotBlank()) partes += "Condición: ${e.condicion}"

        return ReporteRequest(
            description = partes.joinToString("\n"),
            latitude = e.latitud,
            longitude = e.longitud,
            children_quantity = 1,
            children_age = e.edadNino,
            work_type = e.tiposTrabajo.joinToString(", "),
            sighting_time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).format(Date())
        )
    }

    // Deja el error en pantalla. El progreso (id del borrador, fotos ya subidas) se conserva,
    // así que al volver a tocar "Enviar reporte" se continúa desde aquí.
    private fun falloEnPaso(paso: Int, codigo: Int?, detalle: String?) {
        Log.e("SIPINNA", "Envío falló en el paso $paso ($codigo): $detalle")

        val mensaje = if (codigo != null) {
            mensajeDeRespuesta(codigo, detalle)
        } else {
            "Ocurrió un error inesperado."
        }

        _estado.value = _estado.value.copy(
            cargando = false,
            progresoEnvio = "",
            error = mensaje + REINTENTAR
        )
    }

    // Busca el folio del reporte que se acaba de enviar: entre los reportes ya enviados
    // (no DRAFT) del usuario, el que tenga el consecutivo más alto (…-2026-000123).
    private suspend fun folioMasReciente(sesion: String): String {
        return try {
            val respuesta = RetrofitClient.api.misReportes(sesion)
            if (!respuesta.isSuccessful) return ""
            respuesta.body()?.reports.orEmpty()
                .filter { it.report_state != "DRAFT" }
                .maxByOrNull { consecutivoDeFolio(it.folio) }
                ?.folio.orEmpty()
        } catch (ex: Exception) {
            Log.e("SIPINNA", "No se pudo obtener el folio", ex)
            ""
        }
    }

    private fun consecutivoDeFolio(folio: String?): Int {
        return folio?.substringAfterLast("-")?.toIntOrNull() ?: 0
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
                val respuesta = RetrofitClient.api.misReportes(cookieDeSesion(token))

                if (respuesta.isSuccessful) {
                    val reportes = respuesta.body()?.reports ?: emptyList()

                    // El backend no los ordena. El folio termina en un número que crece
                    // con cada reporte ("RIETI-ATIZAPAN-2026-000007"), así que ordenamos
                    // por ese número para dejar arriba los más recientes.
                    // Los borradores (DRAFT) son envíos que no se terminaron: no se muestran.
                    _estado.value = _estado.value.copy(
                        cargandoHistorial = false,
                        historial = reportes
                            .filter { it.report_state != "DRAFT" }
                            .sortedByDescending { consecutivoDeFolio(it.folio) }
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
