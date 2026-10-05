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
// en una de las 3 tarjetas del Figma: en progreso, confirmado o no apto.
fun infoDeEstado(estado: String?): InfoEstado {
    return when (estado) {
        // Ya lo revisaron y le están dando seguimiento
        "en_seguimiento", "canalizado", "concluido", "reincidente" -> InfoEstado(
            titulo = "Reporte confirmado",
            color = VerdeConfirmado,
            detalle = ""   // sin texto: en su lugar la tarjeta muestra el folio
        )
        // Lo descartaron
        "cancelado", "archivado" -> InfoEstado(
            titulo = "No apto",
            color = RojoNoApto,
            detalle = "Tu reporte no pudo ser validado. La información proporcionada no fue suficiente para confirmarlo."
        )
        // Lo están revisando
        "en_revision" -> InfoEstado(
            titulo = "Reporte en progreso",
            color = AmarilloProgreso,
            detalle = "Tu reporte está en revisión. Estamos verificando la información antes de continuar."
        )
        // "registrado" (así empieza todo reporte) o cualquier estado nuevo
        else -> InfoEstado(
            titulo = "Reporte en progreso",
            color = AmarilloProgreso,
            detalle = "Recibimos tu reporte. Pronto empezaremos a revisarlo."
        )
    }
}
