package com.example.sipinnaapp.data

import android.content.Context
import com.example.sipinnaapp.model.ReporteResumen
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

// Copia del último historial que llegó del servidor, para mostrarlo en el Home
// cuando no hay internet. Se borra al cerrar sesión.
object CacheHistorial {

    private const val ARCHIVO = "historial.json"
    private val gson = Gson()
    private val tipoLista = object : TypeToken<List<ReporteResumen>>() {}.type

    private fun archivo(context: Context) = File(context.filesDir, ARCHIVO)

    fun guardar(context: Context, reportes: List<ReporteResumen>) {
        archivo(context).writeText(gson.toJson(reportes))
    }

    fun leer(context: Context): List<ReporteResumen> {
        return try {
            gson.fromJson<List<ReporteResumen>>(archivo(context).readText(), tipoLista) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun borrar(context: Context) {
        archivo(context).delete()
    }
}
