package mx.tec.sipinna.data

import android.content.Context
import mx.tec.sipinna.model.ReporteRequest
import com.google.gson.Gson
import java.io.File
import java.util.UUID

// Un reporte que no se pudo enviar por falta de internet.
// Se guarda dentro de la app (carpeta privada: otras apps no la pueden leer)
// y EnvioPendientesWorker lo manda cuando vuelve la conexión.
data class ReportePendiente(
    val id: String,
    val peticion: ReporteRequest,   // el mismo JSON del paso 1
    val fotos: List<String>,        // rutas de las fotos ya reducidas a JPEG
    val token: String,              // sesión con la que se hizo, CIFRADA ("" = anónimo)
    val creadoEn: Long
)

// Cada reporte pendiente es una carpeta:  files/pendientes/<id>/reporte.json + foto1.jpg + foto2.jpg
object ReportesPendientes {

    private const val CARPETA = "pendientes"
    private const val ARCHIVO = "reporte.json"
    private val gson = Gson()

    private fun base(context: Context) = File(context.filesDir, CARPETA)

    // Las fotos se copian a la app porque el permiso para leerlas de la galería
    // puede perderse si la app se cierra antes de enviarlas
    fun guardar(context: Context, peticion: ReporteRequest, fotos: List<ByteArray>, token: String): ReportePendiente {
        val id = UUID.randomUUID().toString()
        val carpeta = File(base(context), id).apply { mkdirs() }

        val rutas = fotos.mapIndexed { i, bytes ->
            File(carpeta, "foto${i + 1}.jpg").apply { writeBytes(bytes) }.absolutePath
        }

        // El token se guarda cifrado (ver Cifrado.kt)
        val pendiente = ReportePendiente(id, peticion, rutas, Cifrado.cifrar(token), System.currentTimeMillis())

        // El JSON se escribe al final: si la app se cierra a medias, la carpeta
        // sin reporte.json simplemente se ignora
        File(carpeta, ARCHIVO).writeText(gson.toJson(pendiente))
        return pendiente
    }

    // Del más viejo al más nuevo, para enviarlos en el orden en que se hicieron
    fun listar(context: Context): List<ReportePendiente> {
        val carpetas = base(context).listFiles() ?: return emptyList()
        return carpetas.mapNotNull { carpeta ->
            try {
                gson.fromJson(File(carpeta, ARCHIVO).readText(), ReportePendiente::class.java)
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.creadoEn }
    }

    fun borrar(context: Context, id: String) {
        File(base(context), id).deleteRecursively()
    }
}
