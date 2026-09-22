package com.example.sipinnaapp.view

import androidx.compose.ui.graphics.Color
import com.example.sipinnaapp.ui.theme.AmarilloProgreso
import com.example.sipinnaapp.ui.theme.RojoNoApto
import com.example.sipinnaapp.ui.theme.VerdeConfirmado

// Cómo se ve cada estado del reporte en pantalla
data class InfoEstado(
    val titulo: String,
    val color: Color,
    val detalle: String
)

// Convierte el estado que viene de la base de datos (columna "estado")
// en el texto y color que ve el usuario.
fun infoDeEstado(estado: String?): InfoEstado {
    return when (estado?.lowercase()) {
        "confirmado", "validado", "atendido" -> InfoEstado(
            titulo = "Reporte confirmado",
            color = VerdeConfirmado,
            detalle = ""
        )
        "no_apto", "rechazado", "cancelado" -> InfoEstado(
            titulo = "No apto",
            color = RojoNoApto,
            detalle = "Tu reporte no pudo ser validado. La información proporcionada no fue suficiente para confirmarlo."
        )
        // "registrado" (el primero que pone el backend), "en_progreso" o cualquier otro
        else -> InfoEstado(
            titulo = "Reporte en progreso",
            color = AmarilloProgreso,
            detalle = "Tu reporte está en revisión. Estamos verificando la información antes de continuar."
        )
    }
}
