/*
 * SIPINNA — EnvioReporte.kt
 * Capa: Data (almacenamiento local en el teléfono)
 *
 * Envío completo de un reporte guardado (los 4 pasos de la API), usado por el worker de pendientes.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 * Creado: 2026-10-14
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.data

import mx.tec.sipinna.model.ImagenPorSubir
import mx.tec.sipinna.model.RegistroImagenesRequest
import mx.tec.sipinna.model.ReporteRequest
import mx.tec.sipinna.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

/**
 * Resultado de [enviarReporteCompleto]:
 * - [Enviado]: el reporte quedó registrado en el servidor.
 * - [SinConexion]: no hubo internet; se debe reintentar más tarde.
 * - [Rechazado]: el servidor respondió con error ([codigo] HTTP y [detalle]).
 */
sealed class ResultadoEnvio {
    object Enviado : ResultadoEnvio()
    object SinConexion : ResultadoEnvio()                                 // se intenta más tarde
    data class Rechazado(val codigo: Int, val detalle: String?) : ResultadoEnvio()
}

/**
 * Envía un reporte guardado de principio a fin (los mismos 4 pasos que ReporteVM.enviar).
 * sesion = cookie "session_token=..." o null para enviarlo anónimo.
 */
suspend fun enviarReporteCompleto(
    sesion: String?,
    peticion: ReporteRequest,
    fotos: List<File>
): ResultadoEnvio {
    val api = RetrofitClient.api
    try {
        // 1. Crear el borrador
        val r1 = api.crearReporte(sesion, peticion)
        if (!r1.isSuccessful) return ResultadoEnvio.Rechazado(r1.code(), r1.errorBody()?.string())
        val reporteId = r1.body()?.reporteId
            ?: return ResultadoEnvio.Rechazado(r1.code(), "sin reporte_id")
        val llave = r1.body()?.tokenEdicion?.ifBlank { null }

        if (fotos.isNotEmpty()) {
            // 2. Registrar las fotos
            val peticionFotos = RegistroImagenesRequest(
                fotos.indices.map { i -> ImagenPorSubir("foto${i + 1}.jpg", "image/jpeg") }
            )
            val r2 = api.registrarImagenes(sesion, llave, reporteId, peticionFotos)
            if (!r2.isSuccessful) return ResultadoEnvio.Rechazado(r2.code(), r2.errorBody()?.string())
            val ids = r2.body()?.ids.orEmpty()
            if (ids.size != fotos.size) return ResultadoEnvio.Rechazado(r2.code(), "ids de fotos incompletos")

            // 3. Subir cada foto
            for ((i, foto) in fotos.withIndex()) {
                val archivo = MultipartBody.Part.createFormData(
                    "file", foto.name, foto.asRequestBody("image/jpeg".toMediaType())
                )
                val r3 = api.subirImagen(sesion, llave, reporteId, ids[i], archivo)
                if (!r3.isSuccessful || r3.body()?.success == false) {
                    return ResultadoEnvio.Rechazado(r3.code(), r3.errorBody()?.string() ?: r3.body()?.message)
                }
            }
        }

        // 4. Enviar (DRAFT → registrado)
        val r4 = api.enviarReporte(sesion, llave, reporteId)
        if (!r4.isSuccessful) return ResultadoEnvio.Rechazado(r4.code(), r4.errorBody()?.string())

        return ResultadoEnvio.Enviado
    } catch (e: IOException) {
        // Sin internet o se cortó a la mitad. Si ya se había creado el borrador,
        // se queda en DRAFT (no aparece en ningún lado) y el reintento crea uno nuevo.
        return ResultadoEnvio.SinConexion
    }
}
