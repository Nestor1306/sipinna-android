/*
 * SIPINNA — ReporteVM.kt
 * Capa: ViewModel (estado y lógica de las pantallas)
 *
 * Lógica del reporte: captura de los 5 pasos, envío en 4 pasos a la API, modo sin conexión,
 * historial y detalle.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 *   - Néstor Daniel Huidobro Aguilar
 * Creado: 2026-09-13
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import mx.tec.sipinna.model.ImagenPorSubir
import mx.tec.sipinna.model.RegistroImagenesRequest
import mx.tec.sipinna.model.ReporteRequest
import mx.tec.sipinna.network.RetrofitClient
import mx.tec.sipinna.network.cookieDeSesion
import mx.tec.sipinna.network.mensajeDeExcepcion
import mx.tec.sipinna.network.mensajeDeRespuesta
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
import mx.tec.sipinna.data.CacheHistorial
import java.io.IOException
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.work.WorkManager
import mx.tec.sipinna.data.ReportesPendientes
import mx.tec.sipinna.model.ReporteResumen
import mx.tec.sipinna.worker.EnvioPendientesWorker
import mx.tec.sipinna.model.ReporteDetalle
import mx.tec.sipinna.data.CacheDetalles
import mx.tec.sipinna.R
import mx.tec.sipinna.data.BuscadorLugares
import mx.tec.sipinna.data.LugarSugerido
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import mx.tec.sipinna.view.estadoPublico
import mx.tec.sipinna.data.EstadosConocidos

/** Es AndroidViewModel porque necesita el "application" para leer las fotos de la galería */
class ReporteVM(application: Application) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(EstadoReporte())
    /** Estado que observan las pantallas del reporte y el Home. */
    val estado: StateFlow<EstadoReporte> = _estado

    companion object {
        private const val REINTENTAR = " Tu avance se guardó: toca \"Enviar reporte\" para reintentar."

        const val TOTAL_PASOS = 5
        const val MAX_FOTOS = 2              // el diseño permite hasta 2 fotos
        const val MAX_REFERENCIA = 300       // calle o punto de referencia

        /**
         * Por ahora el backend solo acepta enviar (PUT /report/{id}/submit) reportes
         * con al menos una foto subida, así que la pedimos. Si el backend cambia para
         * permitir 0 fotos, basta con poner esto en false.
         */
        const val FOTO_OBLIGATORIA = false
        const val MIN_PALABRAS = 10          // mínimo de palabras en la descripción
        const val OPCION_NO_SE = "No sé"     // en tipo de trabajo, "No sé" no se combina con las demás

        const val LADO_MAXIMO_FOTO = 1600    // pixeles; las fotos se reducen antes de subirlas

        /** Opciones de "Información del NNA" (las mismas que la web de SIPINNA) */
        val OPCIONES_CANTIDAD = listOf("1", "2", "3", "4", "5 o más", "No sé")
        val OPCIONES_EDAD = listOf(
            "0 a 5 años", "6 a 11 años", "12 a 14 años", "15 a 17 años", "No sé"
        )
        val OPCIONES_TRABAJO = listOf(
            "Venta ambulante", "Limpieza de parabrisas", "Mendicidad", "Carga y descarga",
            "Trabajo en comercio", "Campo", "Construcción", "Trabajo doméstico",
            "Recolección de residuos", "Otra actividad", "No sé"
        )

        /** "5 o más" → 5, "No sé" → null (el backend lo entiende como desconocido) */
        fun cantidadParaServidor(opcion: String): Int? = when (opcion) {
            "5 o más" -> 5
            "No sé", "" -> null
            else -> opcion.toIntOrNull()
        }
        /** Cuenta palabras separadas por espacios ("  hola   mundo " = 2) */
        fun contarPalabras(texto: String): Int =
            texto.trim().split(Regex("\\s+")).count { it.isNotBlank() }

        /**
         * Qué le falta al paso actual para poder avanzar ("" = está completo).
         * Las pantallas lo usan para bloquear el botón "Siguiente".
         */
        fun errorDelPaso(e: EstadoReporte): String = when (e.pasoActual) {
            1 -> if (e.direccion.isBlank() && e.latitud == 0.0) "Indica la ubicación del reporte" else ""
            2 -> if (FOTO_OBLIGATORIA && e.fotos.isEmpty()) "Agrega al menos una foto del lugar" else ""
            3 -> when {
                e.peligroInmediato == null -> "Indica si hay peligro inmediato"
                e.cantidadNinos.isEmpty() -> "Indica cuántos niños viste"
                e.edadNino.isEmpty() -> "Selecciona la edad aproximada"
                e.tiposTrabajo.isEmpty() -> "Selecciona al menos un tipo de trabajo"
                e.condicion.isEmpty() -> "Selecciona en qué condición se encuentra"
                else -> ""
            }
            4 -> if (contarPalabras(e.descripcion) < MIN_PALABRAS) "Describe lo que pasó (mínimo $MIN_PALABRAS palabras)" else ""
            else -> ""
        }

        val OPCIONES_CONDICION = listOf(
            "Solo / Sola", "Con adultos", "Con otros niños", "Con familiares", "En situación de riesgo"
        )
    }
    /** Último token con el que se pidió el historial (para recargarlo al volver el internet) */
    private var ultimoToken = ""

    /** Android nos avisa en cuanto el teléfono pierde o recupera la red */
    private val conectividad =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val vigilanteDeRed = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = cambioDeConexion(true)
        override fun onLost(network: Network) = cambioDeConexion(false)
    }

    init {
        _estado.value = _estado.value.copy(sinInternet = !hayInternet())
        conectividad.registerDefaultNetworkCallback(vigilanteDeRed)

        refrescarPendientes()

        // Cada vez que el envío en segundo plano avanza, se actualiza el Home:
        // los pendientes que ya se enviaron desaparecen y aparecen en el historial
        viewModelScope.launch {
            WorkManager.getInstance(application)
                .getWorkInfosForUniqueWorkFlow(EnvioPendientesWorker.NOMBRE)
                .collect { trabajos ->
                    refrescarPendientes()
                    if (trabajos.any { it.state.isFinished } && ultimoToken.isNotBlank()) {
                        cargarHistorial(ultimoToken)
                    }
                }
        }
    }


    override fun onCleared() {
        conectividad.unregisterNetworkCallback(vigilanteDeRed)
        super.onCleared()
    }

    /** ¿El teléfono tiene alguna red con internet (Wi-Fi o datos)? */
    private fun hayInternet(): Boolean {
        val capacidades = conectividad.getNetworkCapabilities(conectividad.activeNetwork) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Se llama cada vez que cambia la red */
    private fun cambioDeConexion(conInternet: Boolean) {
        viewModelScope.launch {
            val antes = _estado.value.sinInternet
            _estado.value = _estado.value.copy(sinInternet = !conInternet)

            if (!conInternet && _estado.value.historial.isEmpty()) {
                // Se fue el internet y no hay nada en pantalla: mostramos la copia guardada
                val copia = withContext(Dispatchers.IO) { CacheHistorial.leer(getApplication()) }
                if (copia.isNotEmpty()) {
                    _estado.value = _estado.value.copy(historial = copia, historialSinConexion = true)
                }
            } else if (conInternet && antes && ultimoToken.isNotBlank()) {
                // Volvió el internet: actualizamos con los datos del servidor
                cargarHistorial(ultimoToken)
            }
        }
    }

    // --- Paso 1: Locación ---

    /** Guarda la dirección escrita en el buscador. */
    fun actualizarDireccion(valor: String) {
        _estado.value = _estado.value.copy(direccion = valor)
    }

    /** Guarda la calle o punto de referencia (máximo [MAX_REFERENCIA] caracteres). */
    fun actualizarReferencia(valor: String) {
        _estado.value = _estado.value.copy(referencia = valor.take(MAX_REFERENCIA))
    }

    /** Guarda la ubicación del pin. */
    fun actualizarCoordenadas(lat: Double, lng: Double) {
        _estado.value = _estado.value.copy(latitud = lat, longitud = lng, buscandoUbicacion = false)
    }

    /** Indica si se está obteniendo la ubicación actual (muestra el indicador). */
    fun actualizarBuscandoUbicacion(valor: Boolean) {
        _estado.value = _estado.value.copy(buscandoUbicacion = valor)
    }
    // --- Buscador de lugares (paso 1) ---

    private var busquedaLugar: Job? = null

    /** Se llama cada vez que el usuario escribe en el campo de dirección */
    fun buscarLugares(texto: String) {
        busquedaLugar?.cancel()
        if (texto.trim().length < 3) {
            _estado.value = _estado.value.copy(sugerencias = emptyList())
            return
        }
        busquedaLugar = viewModelScope.launch {
            delay(400)   // espera a que deje de escribir para no buscar en cada letra
            val e = _estado.value
            val token = getApplication<Application>().getString(R.string.mapbox_access_token).trim()
            val resultados = try {
                withContext(Dispatchers.IO) {
                    BuscadorLugares.buscar(
                        texto.trim(), token,
                        e.latitud.takeIf { it != 0.0 }, e.longitud.takeIf { it != 0.0 }
                    )
                }
            } catch (ex: Exception) {
                Log.w("SIPINNA", "No se pudo buscar el lugar", ex)
                emptyList()   // sin internet: simplemente no salen sugerencias
            }
            _estado.value = _estado.value.copy(sugerencias = resultados)
        }
    }

    /** Tocó una sugerencia: mueve el pin ahí y llena la dirección */
    fun elegirLugar(lugar: LugarSugerido) {
        busquedaLugar?.cancel()
        _estado.value = _estado.value.copy(
            latitud = lugar.latitud,
            longitud = lugar.longitud,
            direccion = lugar.direccion.ifBlank { lugar.nombre },
            sugerencias = emptyList()
        )
    }

    /** Oculta la lista de sugerencias del buscador. */
    fun cerrarSugerencias() {
        busquedaLugar?.cancel()
        _estado.value = _estado.value.copy(sugerencias = emptyList())
    }

    // --- Paso 2: Fotos ---

    /** Agrega fotos (URIs de cámara o galería) sin repetir y sin pasar de [MAX_FOTOS]. */
    fun agregarFotos(uris: List<String>) {
        val nuevas = (_estado.value.fotos + uris).distinct().take(MAX_FOTOS)
        _estado.value = _estado.value.copy(fotos = nuevas)
    }

    /** Quita una foto elegida. */
    fun quitarFoto(uri: String) {
        _estado.value = _estado.value.copy(fotos = _estado.value.fotos - uri)
    }

    // --- Paso 3: Información del niño ---

    /** Guarda el rango de edad elegido. */
    fun seleccionarEdad(valor: String) {
        _estado.value = _estado.value.copy(edadNino = valor)
    }
    /** Guarda cuántos NNA vio ("1"…"5 o más" o "No sé"). */
    fun seleccionarCantidad(valor: String) {
        _estado.value = _estado.value.copy(cantidadNinos = valor)
    }

    /** Guarda si hay peligro inmediato. */
    fun seleccionarPeligro(valor: Boolean) {
        _estado.value = _estado.value.copy(peligroInmediato = valor)
    }

    /**
     * Se pueden elegir varios: si ya estaba, se quita; si no, se agrega
     * "No sé" va sola: al elegirla se quitan las demás, y al elegir otra se quita "No sé"
     */
    fun alternarTipoTrabajo(valor: String) {
        val actuales = _estado.value.tiposTrabajo
        val nuevos = when {
            valor in actuales -> actuales - valor
            valor == OPCION_NO_SE -> setOf(OPCION_NO_SE)
            else -> actuales - OPCION_NO_SE + valor
        }
        _estado.value = _estado.value.copy(tiposTrabajo = nuevos)
    }

    /** Si se toca la opción ya elegida, se quita (la pregunta es opcional) */
    fun seleccionarRiesgo(valor: String) {
        val actual = _estado.value.riesgo
        _estado.value = _estado.value.copy(riesgo = if (actual == valor) "" else valor)
    }

    /** Guarda la condición en que se encuentra el NNA. */
    fun seleccionarCondicion(valor: String) {
        _estado.value = _estado.value.copy(condicion = valor)
    }

    // --- Paso 4: Descripción ---

    /** Guarda la descripción escrita. */
    fun actualizarDescripcion(valor: String) {
        _estado.value = _estado.value.copy(descripcion = valor)
    }

    fun cambiarFechaAvistamiento(millis: Long) {
        if (millis > System.currentTimeMillis()) {
            _estado.value = _estado.value.copy(error = "La fecha y hora no pueden ser posteriores a este momento.")
            return
        }
        _estado.value = sinProgresoDeEnvio(_estado.value).copy(fechaAvistamiento = millis)
    }

    // --- Navegación entre pasos ---

    /**
     * Avanza al siguiente paso si el actual está completo (ver [errorDelPaso]).
     * Si se estaba corrigiendo un paso desde la Confirmación, regresa a ella.
     */
    fun siguientePaso() {
        val e = _estado.value

        // Validación del paso en el que estamos antes de avanzar
        // (los botones ya se bloquean con esto mismo; aquí es una segunda revisión)
        val errorPaso = errorDelPaso(e)
        if (errorPaso.isNotEmpty()) {
            _estado.value = e.copy(error = errorPaso)
            return
        }

        // Si vino a corregir algo desde la Confirmación, regresa directo a ella
        if (e.editandoDesdeConfirmar) {
            _estado.value = e.copy(pasoActual = TOTAL_PASOS, editandoDesdeConfirmar = false)
            return
        }

        if (e.pasoActual < TOTAL_PASOS) {
            _estado.value = e.copy(pasoActual = e.pasoActual + 1)
        }
    }

    /** Regresa al paso anterior (o a la Confirmación si se estaba corrigiendo un paso). */
    fun pasoAnterior() {
        val e = _estado.value
        // La flecha también regresa a la Confirmación si se estaba editando un paso
        if (e.editandoDesdeConfirmar && errorDelPaso(e).isEmpty()) {
            _estado.value = e.copy(pasoActual = TOTAL_PASOS, editandoDesdeConfirmar = false)
            return
        }
        if (e.pasoActual > 1) {
            _estado.value = sinProgresoDeEnvio(e).copy(
                pasoActual = e.pasoActual - 1,
                editandoDesdeConfirmar = false
            )
        }
    }

    /** "Editar reporte" (abajo en la Confirmación): recorre todo desde el paso que se pida */
    fun irAPaso(paso: Int) {
        _estado.value = sinProgresoDeEnvio(_estado.value).copy(
            pasoActual = paso.coerceIn(1, TOTAL_PASOS),
            editandoDesdeConfirmar = false
        )
    }

    /** Botón "Editar" de una sección de la Confirmación: va a ese paso y al terminar regresa */
    fun editarDesdeConfirmar(paso: Int) {
        _estado.value = sinProgresoDeEnvio(_estado.value).copy(
            pasoActual = paso.coerceIn(1, TOTAL_PASOS - 1),
            editandoDesdeConfirmar = true
        )
    }

    /**
     * Si el usuario regresa a editar después de un envío que falló a la mitad, el borrador
     * que ya existe en el servidor tiene los datos viejos. Lo olvidamos para que el
     * siguiente "Enviar" cree uno nuevo con los datos corregidos. (El borrador viejo se
     * queda en DRAFT: no aparece en el historial ni lo analiza Jev.)
     */
    private fun sinProgresoDeEnvio(e: EstadoReporte): EstadoReporte {
        if (e.reporteIdBorrador.isBlank()) return e
        return e.copy(
            reporteIdBorrador = "", tokenEdicion = "", folioBorrador = "",
            idsImagenes = emptyList(), fotosSubidas = emptySet()
        )
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

    /**
     * Envía el reporte en los 4 pasos descritos arriba. Sin internet, lo guarda en
     * [ReportesPendientes] para que [EnvioPendientesWorker] lo mande después.
     * @param token sesión del usuario; "" para enviarlo anónimo.
     */
    fun enviar(token: String) {
        val e = _estado.value
        if (e.cargando) return   // evita enviar dos veces con doble toque

        // Sin sesión el reporte es anónimo: no se manda cookie y el backend responde una
        // llave ("edit_token") que se usa en los pasos 2 a 4 en lugar de la sesión.
        val anonimo = token.isBlank()
        val sesion: String? = if (anonimo) null else cookieDeSesion(token)

        // Sin internet ni lo intentamos: se guarda en el teléfono y se envía solo después
        if (!hayInternet()) {
            viewModelScope.launch { guardarParaDespues(e, token) }
            return
        }

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
                    val llave = respuesta.body()?.tokenEdicion.orEmpty()
                    if (anonimo && llave.isBlank()) {
                        return@launch falloEnPaso(1, null, "El servidor no regresó la llave del reporte anónimo")
                    }
                    _estado.value = _estado.value.copy(
                        reporteIdBorrador = reporteId,
                        tokenEdicion = llave,
                        folioBorrador = respuesta.body()?.folio.orEmpty()
                        // La clave de consulta no se guarda ni se muestra: los reportes anónimos
                        // no tienen seguimiento y los de cuenta se siguen desde el inicio.
                    )
                }
                val llave: String? = _estado.value.tokenEdicion.ifBlank { null }

                // ---- Paso 2: registrar las fotos ----
                var ids = _estado.value.idsImagenes
                if (e.fotos.isNotEmpty() && ids.isEmpty()) {
                    _estado.value = _estado.value.copy(progresoEnvio = "Preparando fotos…")

                    val peticion = RegistroImagenesRequest(
                        e.fotos.indices.map { i -> ImagenPorSubir("foto${i + 1}.jpg", "image/jpeg") }
                    )
                    val respuesta = RetrofitClient.api.registrarImagenes(sesion, llave, reporteId, peticion)
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
                    val respuesta = RetrofitClient.api.subirImagen(sesion, llave, reporteId, imagenId, archivo)
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
                val respuesta = RetrofitClient.api.enviarReporte(sesion, llave, reporteId)
                if (!respuesta.isSuccessful) {
                    return@launch falloEnPaso(4, respuesta.code(), respuesta.errorBody()?.string())
                }

                // El folio lo asignó el servidor en el paso 1. Si un servidor viejo no lo
                // regresó, se busca en el historial (solo funciona con cuenta).
                val folio = _estado.value.folioBorrador.ifBlank {
                    if (sesion != null) folioMasReciente(sesion) else ""
                }

                _estado.value = _estado.value.copy(
                    cargando = false,
                    progresoEnvio = "",
                    enviadoAnonimo = anonimo,
                    folioGenerado = when {
                        folio.isNotBlank() -> folio
                        anonimo -> "Anónimo"
                        else -> "Consúltalo en tu historial"
                    },
                    estadoGenerado = "registrado",   // todo reporte enviado empieza así
                    avisoFotos = ""
                )
            } catch (ex: IOException) {
                // Se cayó el internet a medio envío: lo guardamos para enviarlo después
                Log.e("SIPINNA", "Sin conexión al enviar, se guarda como pendiente", ex)
                guardarParaDespues(e, token)

            } catch (ex: Exception) {
                _estado.value = _estado.value.copy(
                    cargando = false,
                    progresoEnvio = "",
                    error = mensajeDeExcepcion(ex) + REINTENTAR
                )
            }
        }
    }
    /**
     * Guarda el reporte (con sus fotos ya reducidas) en el teléfono y le pide a
     * WorkManager que lo envíe en cuanto haya conexión
     */
    private suspend fun guardarParaDespues(e: EstadoReporte, token: String) {
        val app = getApplication<Application>()
        _estado.value = _estado.value.copy(cargando = true, error = "", progresoEnvio = "Guardando reporte…")

        try {
            val fotos = e.fotos.mapNotNull { prepararFoto(it) }
            withContext(Dispatchers.IO) {
                ReportesPendientes.guardar(app, armarPeticion(e), fotos, token)
            }
            EnvioPendientesWorker.programar(app)
            refrescarPendientes()

            _estado.value = _estado.value.copy(
                cargando = false,
                progresoEnvio = "",
                guardadoSinConexion = true,
                enviadoAnonimo = token.isBlank(),
                folioGenerado = "Pendiente de envío",   // muestra la pantalla final
                estadoGenerado = "pendiente_envio"
            )
        } catch (ex: Exception) {
            Log.e("SIPINNA", "No se pudo guardar el reporte pendiente", ex)
            _estado.value = _estado.value.copy(
                cargando = false,
                progresoEnvio = "",
                error = "No hay conexión y no se pudo guardar el reporte en el teléfono. Inténtalo de nuevo."
            )
        }
    }

    /** Lee del teléfono los reportes que faltan por enviar (para mostrarlos en el Home) */
    fun refrescarPendientes() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val pendientes = withContext(Dispatchers.IO) { ReportesPendientes.listar(app) }
            _estado.value = _estado.value.copy(
                pendientes = pendientes.reversed().map { p ->   // el más nuevo arriba
                    ReporteResumen(
                        folio = null,
                        report_state = "pendiente_envio",
                        latitude = p.peticion.latitude,
                        longitude = p.peticion.longitude,
                        description = p.peticion.description,
                        idLocal = p.id
                    )
                }
            )
        }
    }
    /** Abre el detalle de un reporte guardado sin internet, con lo que hay en el teléfono */
    fun abrirPendiente(idLocal: String) {
        _estado.value = _estado.value.copy(detalle = null, cargandoDetalle = true, errorDetalle = "")

        viewModelScope.launch {
            val app = getApplication<Application>()
            val pendiente = withContext(Dispatchers.IO) {
                ReportesPendientes.listar(app).firstOrNull { it.id == idLocal }
            }

            if (pendiente == null) {
                // Pudo haberse enviado justo ahora
                _estado.value = _estado.value.copy(
                    cargandoDetalle = false,
                    errorDetalle = "Este reporte ya se envió. Regresa al inicio para verlo en tu historial."
                )
                return@launch
            }

            val p = pendiente.peticion
            val guardadoEl = java.time.Instant.ofEpochMilli(pendiente.creadoEn)
                .atZone(java.time.ZoneId.systemDefault())
                .toOffsetDateTime()
                .toString()

            _estado.value = _estado.value.copy(
                cargandoDetalle = false,
                detalle = ReporteDetalle(
                    folio = null,                          // aún no tiene: lo asigna el servidor
                    description = p.description,
                    latitude = p.latitude,
                    longitude = p.longitude,
                    children_quantity = p.children_quantity,
                    children_age = p.children_age,
                    work_type = p.work_type,
                    created_at = guardadoEl,
                    sighting_time = p.sighting_time,
                    zone_name = null,
                    last_state = "pendiente_envio",
                    state_changed_at = guardadoEl,
                    images = pendiente.fotos.map { "file://$it" }   // fotos guardadas en el teléfono
                )
            )
        }
    }

    /**
     * Arma el JSON del paso 1. La dirección escrita y la condición no tienen columna en la
     * base, así que van al final de la descripción para no perderlas.
     */
    private fun armarPeticion(e: EstadoReporte): ReporteRequest {
        val partes = mutableListOf(e.descripcion.trim())
        if (e.direccion.isNotBlank()) partes += "Dirección: ${e.direccion.trim()}"
        if (e.condicion.isNotBlank()) partes += "Condición: ${e.condicion}"

        return ReporteRequest(
            description = partes.joinToString("\n"),
            latitude = e.latitud,
            longitude = e.longitud,
            children_quantity = cantidadParaServidor(e.cantidadNinos),
            children_age = e.edadNino,
            work_type = e.tiposTrabajo.joinToString(", "),
            sighting_time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                .format(Date(e.fechaAvistamiento ?: System.currentTimeMillis())),
            immediate_danger = e.peligroInmediato == true,
            risk = e.riesgo.ifBlank { null },
            reference = e.referencia.trim().ifBlank { null }
        )
    }

    /**
     * Deja el error en pantalla. El progreso (id del borrador, fotos ya subidas) se conserva,
     * así que al volver a tocar "Enviar reporte" se continúa desde aquí.
     */
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

    /**
     * Busca el folio del reporte que se acaba de enviar: entre los reportes ya enviados
     * (no DRAFT) del usuario, el que tenga el consecutivo más alto (…-2026-000123).
     */
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

    /**
     * Lee la foto de la galería, la hace más pequeña y la convierte a JPEG.
     * Así se sube más rápido y el servidor siempre recibe el mismo formato.
     */
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

    /**
     * Pide los reportes del usuario (GET /report), guarda una copia para verlos sin internet
     * y registra sus estados como vistos (para las notificaciones).
     * @param token sesión del usuario; si está vacío (anónimo) no hace nada.
     */
    fun cargarHistorial(token: String) {
        // Los anónimos no tienen historial
        if (token.isBlank()) return

        val app = getApplication<Application>()
        ultimoToken = token

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
                    val historial = reportes
                        .filter { estadoPublico(it.report_state) != "borrador" }   // DRAFT o "borrador"
                        .sortedByDescending { consecutivoDeFolio(it.folio) }

                    // Copia en el teléfono para poder verlo sin internet
                    withContext(Dispatchers.IO) {
                        runCatching { CacheHistorial.guardar(app, historial) }
                        // Lo que el usuario ya vio en la app: no se le avisa con notificación
                        runCatching { EstadosConocidos.guardar(app, historial) }
                    }

                    _estado.value = _estado.value.copy(
                        cargandoHistorial = false,
                        historialSinConexion = false,
                        historial = historial
                    )
                } else if (respuesta.code() == 401) {
                    // La sesión caducó: MainActivity cierra la sesión y manda al Login
                    _estado.value = _estado.value.copy(cargandoHistorial = false, sesionVencida = true)
                } else {
                    _estado.value = _estado.value.copy(
                        cargandoHistorial = false,
                        errorHistorial = mensajeDeRespuesta(respuesta.code(), respuesta.errorBody()?.string())
                    )
                }
            } catch (ex: IOException) {
                // Sin internet: mostramos la última copia guardada, si hay
                val copia = withContext(Dispatchers.IO) { CacheHistorial.leer(app) }
                _estado.value = if (copia.isNotEmpty()) {
                    _estado.value.copy(cargandoHistorial = false, historial = copia, historialSinConexion = true)
                } else {
                    _estado.value.copy(cargandoHistorial = false, errorHistorial = mensajeDeExcepcion(ex))
                }
            } catch (ex: Exception) {
                _estado.value = _estado.value.copy(
                    cargandoHistorial = false,
                    errorHistorial = mensajeDeExcepcion(ex)
                )
            }
        }
    }

    /**
     * Pide a la base de datos el detalle de un reporte (al tocar su tarjeta en el Home).
     * Sin internet, muestra la copia guardada (o al menos lo que ya se ve en la tarjeta).
     */
    fun cargarDetalle(token: String, folio: String) {
        _estado.value = _estado.value.copy(
            detalle = null, cargandoDetalle = true, errorDetalle = "", detalleSinConexion = false
        )

        if (token.isBlank()) {
            _estado.value = _estado.value.copy(
                cargandoDetalle = false,
                errorDetalle = "Inicia sesión para ver el detalle de tus reportes."
            )
            return
        }

        viewModelScope.launch {
            val app = getApplication<Application>()
            try {
                val respuesta = RetrofitClient.api.detalleReporte(cookieDeSesion(token), folio)
                if (respuesta.code() == 401) {
                    _estado.value = _estado.value.copy(cargandoDetalle = false, sesionVencida = true)
                    return@launch
                }
                val detalle = respuesta.body()?.report

                if (respuesta.isSuccessful && detalle != null) {
                    // Copia para verlo después sin internet
                    withContext(Dispatchers.IO) { runCatching { CacheDetalles.guardar(app, detalle) } }
                    _estado.value = _estado.value.copy(cargandoDetalle = false, detalle = detalle)
                } else {
                    _estado.value = _estado.value.copy(
                        cargandoDetalle = false,
                        errorDetalle = mensajeDeRespuesta(respuesta.code(), respuesta.errorBody()?.string())
                    )
                }
            } catch (ex: IOException) {
                // Sin internet: copia guardada del detalle, o lo que ya se ve en la tarjeta
                val copia = withContext(Dispatchers.IO) { CacheDetalles.leer(app, folio) }
                    ?: detalleDesdeTarjeta(folio)

                _estado.value = if (copia != null) {
                    _estado.value.copy(cargandoDetalle = false, detalle = copia, detalleSinConexion = true)
                } else {
                    _estado.value.copy(cargandoDetalle = false, errorDetalle = mensajeDeExcepcion(ex))
                }
            } catch (ex: Exception) {
                _estado.value = _estado.value.copy(
                    cargandoDetalle = false,
                    errorDetalle = mensajeDeExcepcion(ex)
                )
            }
        }
    }

    /** Arma un detalle mínimo con los datos de la tarjeta del historial */
    private fun detalleDesdeTarjeta(folio: String): ReporteDetalle? {
        val r = _estado.value.historial.firstOrNull { it.folio == folio } ?: return null
        return ReporteDetalle(
            folio = r.folio,
            description = r.description,
            latitude = r.latitude,
            longitude = r.longitude,
            children_quantity = null,
            children_age = null,
            work_type = null,
            created_at = null,
            sighting_time = null,
            zone_name = null,
            last_state = r.report_state,
            state_changed_at = null,
            images = emptyList()
        )
    }

    /** Al cerrar sesión se borra todo, para que otro usuario no vea estos reportes */
    fun limpiarTodo() {
        ultimoToken = ""
        CacheHistorial.borrar(getApplication())
        EstadosConocidos.borrar(getApplication())
        CacheDetalles.borrarTodo(getApplication())
        _estado.value = EstadoReporte(
            sinInternet = _estado.value.sinInternet,
            pendientes = _estado.value.pendientes
        )
    }

    /** Oculta el mensaje de error. */
    fun cerrarError() {
        _estado.value = _estado.value.copy(error = "")
    }

    /**
     * Limpia el formulario pero conserva el historial de reportes enviados.
     * (Al volver al Home, el historial se vuelve a pedir a la base de datos.)
     */
    fun nuevoReporte() {
        val e = _estado.value
        _estado.value = EstadoReporte(
            historial = e.historial,
            historialSinConexion = e.historialSinConexion,
            sinInternet = e.sinInternet,
            pendientes = e.pendientes
        )
    }
}

