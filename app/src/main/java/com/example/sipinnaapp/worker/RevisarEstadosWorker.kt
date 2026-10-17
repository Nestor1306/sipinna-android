package com.example.sipinnaapp.worker

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.sipinnaapp.MainActivity
import com.example.sipinnaapp.data.EstadosConocidos
import com.example.sipinnaapp.data.SessionManager
import com.example.sipinnaapp.network.RetrofitClient
import com.example.sipinnaapp.network.cookieDeSesion
import com.example.sipinnaapp.view.infoDeEstado
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.example.sipinnaapp.view.estadoPublico
import android.util.Log

// Cada ~15 minutos (aunque la app esté cerrada) revisa si algún reporte cambió de estado
// y, si sí, avisa con una notificación.
class RevisarEstadosWorker(contexto: Context, parametros: WorkerParameters) :
    CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        val token = SessionManager(app).leerSesion().token
        if (token.isBlank()) return Result.success()   // sin sesión (o anónimo): nada que revisar

        val respuesta = try {
            RetrofitClient.api.misReportes(cookieDeSesion(token))
        } catch (e: IOException) {
            return Result.retry()        // sin internet: se intenta más tarde
        } catch (e: Exception) {
            return Result.success()      // otro error: se intenta en la siguiente vuelta
        }
        // 401 (sesión vencida) u otro error del servidor: no se avisa nada
        if (!respuesta.isSuccessful) return Result.success()

        val reportes = (respuesta.body()?.reports ?: emptyList())
            .filter { estadoPublico(it.report_state) != "borrador" }

        val anteriores = EstadosConocidos.leer(app)

        // La primera vez solo se guardan los estados; no hay con qué comparar
        if (anteriores != null) {
            for (reporte in reportes) {
                val folio = reporte.folio ?: continue
                val estado = reporte.report_state ?: continue
                val antes = anteriores[folio] ?: continue   // reporte nuevo: no se avisa
                // Se compara ya traducido: "registrado" (viejo) y "recibido" (nuevo) son lo mismo
                if (estadoPublico(estado) != estadoPublico(antes)) {
                    Log.i("SIPINNA", "Cambió $folio: $antes → $estado")
                    avisar(app, folio, estado)
                }
            }
        }
        EstadosConocidos.guardar(app, reportes)
        return Result.success()
    }

    companion object {
        private const val NOMBRE = "revisar_estados"
        private const val CANAL = "estado_reportes"

        // Se llama al abrir la app. KEEP: si ya estaba programado, no lo duplica.
        fun programar(context: Context) {
            val tarea = PeriodicWorkRequestBuilder<RevisarEstadosWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NOMBRE, ExistingPeriodicWorkPolicy.KEEP, tarea)
        }

        private fun crearCanal(context: Context) {
            val canal = NotificationChannel(
                CANAL, "Estado de mis reportes", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Avisos cuando uno de tus reportes cambia de estado" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }

        @SuppressLint("MissingPermission")   // el permiso se revisa aquí mismo
        private fun avisar(context: Context, folio: String, estado: String) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return

            crearCanal(context)
            val info = infoDeEstado(estado)

            // Al tocarla se abre la app
            val abrirApp = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notificacion = NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Tu reporte cambió a: ${info.titulo}")
                .setContentText(info.detalle)
                .setStyle(NotificationCompat.BigTextStyle().bigText("${info.detalle}\nFolio: $folio"))
                // En la pantalla bloqueada no se muestra el contenido (es información sensible)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setContentIntent(abrirApp)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(folio.hashCode(), notificacion)
        }
    }
}