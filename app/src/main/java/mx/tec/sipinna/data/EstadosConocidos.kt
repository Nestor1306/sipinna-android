/*
 * SIPINNA — EstadosConocidos.kt
 * Capa: Data (almacenamiento local en el teléfono)
 *
 * Último estado visto de cada reporte, para detectar cambios y notificar.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 * Creado: 2026-10-15
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.data

import android.content.Context
import mx.tec.sipinna.model.ReporteResumen
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Último estado que el usuario "ya vio" de cada reporte: folio → estado.
 * Sirve para saber si un reporte cambió y avisar con una notificación.
 * Va en noBackupFilesDir: Android nunca lo incluye en los respaldos.
 */
object EstadosConocidos {

    private const val ARCHIVO = "estados_conocidos.json"
    private val gson = Gson()

    private fun archivo(context: Context) = File(context.noBackupFilesDir, ARCHIVO)

    /** null = todavía no hay nada guardado (primera vez) */
    fun leer(context: Context): Map<String, String>? {
        return try {
            val tipo = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson<Map<String, String>>(archivo(context).readText(), tipo)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Guarda el estado actual de cada reporte como "ya visto".
     * @param reportes historial que regresó GET /report.
     */
    fun guardar(context: Context, reportes: List<ReporteResumen>) {
        val mapa = reportes.mapNotNull { r ->
            val folio = r.folio ?: return@mapNotNull null
            val estado = r.report_state ?: return@mapNotNull null
            folio to estado
        }.toMap()
        archivo(context).writeText(gson.toJson(mapa))
    }

    /** Borra los estados guardados (se llama al cerrar sesión). */
    fun borrar(context: Context) {
        archivo(context).delete()
    }
}