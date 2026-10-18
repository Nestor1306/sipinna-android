/*
 * SIPINNA — CacheDetalles.kt
 * Capa: Data (almacenamiento local en el teléfono)
 *
 * Copia local del detalle de cada reporte para consultarlo sin internet.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 * Creado: 2026-10-14
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.data

import android.content.Context
import mx.tec.sipinna.model.ReporteDetalle
import com.google.gson.Gson
import java.io.File
import java.net.URLEncoder

/**
 * Copia del detalle de cada reporte que se abrió con internet, para poder verlo
 * después sin conexión. Se borra al cerrar sesión.
 * Cada detalle es un archivo:  files/detalles/<folio>.json
 */
object CacheDetalles {

    private const val CARPETA = "detalles"
    private val gson = Gson()

    private fun carpeta(context: Context) = File(context.filesDir, CARPETA)

    /** El folio trae espacios y acentos: se codifica para usarlo como nombre de archivo */
    private fun archivo(context: Context, folio: String) =
        File(carpeta(context), URLEncoder.encode(folio, "UTF-8") + ".json")

    /**
     * Guarda la copia de un detalle (sin los enlaces de fotos, que caducan).
     * @param detalle detalle recibido del servidor; si no trae folio no se guarda.
     */
    fun guardar(context: Context, detalle: ReporteDetalle) {
        val folio = detalle.folio ?: return
        carpeta(context).mkdirs()
        // Los enlaces de las fotos caducan en 15 minutos: no tiene caso guardarlos
        archivo(context, folio).writeText(gson.toJson(detalle.copy(images = emptyList())))
    }

    /**
     * Lee la copia guardada de un reporte.
     * @param folio folio del reporte.
     * @return el detalle guardado, o null si no existe o el archivo está dañado.
     */
    fun leer(context: Context, folio: String): ReporteDetalle? {
        return try {
            gson.fromJson(archivo(context, folio).readText(), ReporteDetalle::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /** Borra todas las copias (se llama al cerrar sesión). */
    fun borrarTodo(context: Context) {
        carpeta(context).deleteRecursively()
    }
}