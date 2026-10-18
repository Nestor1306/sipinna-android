package mx.tec.sipinna.data

import android.content.Context
import mx.tec.sipinna.model.ReporteDetalle
import com.google.gson.Gson
import java.io.File
import java.net.URLEncoder

// Copia del detalle de cada reporte que se abrió con internet, para poder verlo
// después sin conexión. Se borra al cerrar sesión.
// Cada detalle es un archivo:  files/detalles/<folio>.json
object CacheDetalles {

    private const val CARPETA = "detalles"
    private val gson = Gson()

    private fun carpeta(context: Context) = File(context.filesDir, CARPETA)

    // El folio trae espacios y acentos: se codifica para usarlo como nombre de archivo
    private fun archivo(context: Context, folio: String) =
        File(carpeta(context), URLEncoder.encode(folio, "UTF-8") + ".json")

    fun guardar(context: Context, detalle: ReporteDetalle) {
        val folio = detalle.folio ?: return
        carpeta(context).mkdirs()
        // Los enlaces de las fotos caducan en 15 minutos: no tiene caso guardarlos
        archivo(context, folio).writeText(gson.toJson(detalle.copy(images = emptyList())))
    }

    fun leer(context: Context, folio: String): ReporteDetalle? {
        return try {
            gson.fromJson(archivo(context, folio).readText(), ReporteDetalle::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun borrarTodo(context: Context) {
        carpeta(context).deleteRecursively()
    }
}