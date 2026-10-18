package mx.tec.sipinna.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import mx.tec.sipinna.data.ReportesPendientes
import mx.tec.sipinna.data.ResultadoEnvio
import mx.tec.sipinna.data.enviarReporteCompleto
import mx.tec.sipinna.network.cookieDeSesion
import java.io.File
import java.util.concurrent.TimeUnit
import mx.tec.sipinna.data.Cifrado

// Envía los reportes que se guardaron sin internet.
// WorkManager lo ejecuta en cuanto hay conexión, aunque la app esté cerrada,
// y lo vuelve a intentar más tarde si algo falla.
class EnvioPendientesWorker(contexto: Context, parametros: WorkerParameters) :
    CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val pendientes = ReportesPendientes.listar(applicationContext)
        var algunoRechazado = false

        for (pendiente in pendientes) {
            val fotos = pendiente.fotos.map { File(it) }.filter { it.exists() }
            // El token viene cifrado; los pendientes de versiones anteriores lo traen sin cifrar
            val token = Cifrado.descifrar(pendiente.token) ?: pendiente.token
            val sesion = token.ifBlank { null }?.let { cookieDeSesion(it) }

            var resultado = enviarReporteCompleto(sesion, pendiente.peticion, fotos)

            // Si la sesión ya caducó (401), mejor enviarlo anónimo que perder el reporte
            if (resultado is ResultadoEnvio.Rechazado && resultado.codigo == 401 && sesion != null) {
                resultado = enviarReporteCompleto(null, pendiente.peticion, fotos)
            }

            when (resultado) {
                is ResultadoEnvio.Enviado -> {
                    Log.i("SIPINNA", "Reporte pendiente ${pendiente.id} enviado")
                    ReportesPendientes.borrar(applicationContext, pendiente.id)
                }
                // Se volvió a ir el internet: se intenta todo más tarde
                is ResultadoEnvio.SinConexion -> return Result.retry()
                is ResultadoEnvio.Rechazado -> {
                    Log.e("SIPINNA", "El servidor rechazó el pendiente ${pendiente.id} (${resultado.codigo}): ${resultado.detalle}")
                    algunoRechazado = true
                }
            }
        }

        // Si el servidor falló (p. ej. un 500 o S3), se reintenta unas veces más.
        // Los que sigan sin poder enviarse se quedan guardados y se intentan
        // otra vez la próxima vez que se abra la app.
        return if (algunoRechazado && runAttemptCount < MAX_INTENTOS) Result.retry() else Result.success()
    }

    companion object {
        const val NOMBRE = "enviar-reportes-pendientes"
        private const val MAX_INTENTOS = 5

        // Pide a Android que ejecute el envío cuando haya internet
        fun programar(contexto: Context) {
            val peticion = OneTimeWorkRequestBuilder<EnvioPendientesWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            // APPEND_OR_REPLACE: si ya hay un envío en curso, este se forma detrás de él
            // (así no se pierde un reporte que se guardó mientras el otro estaba enviando)
            WorkManager.getInstance(contexto)
                .enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.APPEND_OR_REPLACE, peticion)
        }
    }
}
