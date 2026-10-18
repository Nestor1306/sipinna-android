/*
 * SIPINNA — CacheHistorial.kt
 * Capa: Data (almacenamiento local en el teléfono)
 *
 * Copia local del historial de reportes para mostrar el Home sin internet.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 * Creado: 2026-10-14
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
 * Copia del último historial que llegó del servidor, para mostrarlo en el Home
 * cuando no hay internet. Se borra al cerrar sesión.
 */
object CacheHistorial {

    private const val ARCHIVO = "historial.json"
    private val gson = Gson()
    private val tipoLista = object : TypeToken<List<ReporteResumen>>() {}.type

    private fun archivo(context: Context) = File(context.filesDir, ARCHIVO)

    /**
     * Reemplaza la copia del historial.
     * @param reportes lista que regresó GET /report.
     */
    fun guardar(context: Context, reportes: List<ReporteResumen>) {
        archivo(context).writeText(gson.toJson(reportes))
    }

    /** @return el último historial guardado, o lista vacía si no hay o está dañado. */
    fun leer(context: Context): List<ReporteResumen> {
        return try {
            gson.fromJson<List<ReporteResumen>>(archivo(context).readText(), tipoLista) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Borra la copia (se llama al cerrar sesión). */
    fun borrar(context: Context) {
        archivo(context).delete()
    }
}
