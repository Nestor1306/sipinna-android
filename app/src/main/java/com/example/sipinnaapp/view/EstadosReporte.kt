package com.example.sipinnaapp.view

import androidx.compose.ui.graphics.Color
import com.example.sipinnaapp.ui.theme.AmarilloProgreso
import com.example.sipinnaapp.ui.theme.AzulInstitucional
import com.example.sipinnaapp.ui.theme.GrisIcono
import com.example.sipinnaapp.ui.theme.Magenta
import com.example.sipinnaapp.ui.theme.Morado
import com.example.sipinnaapp.ui.theme.RojoNoApto
import com.example.sipinnaapp.ui.theme.TextoSecundario
import com.example.sipinnaapp.ui.theme.VerdeConfirmado

// Los colores de cada estado son los mismos que usa la web (dashboard de www.sipinna.com)

// Cómo se ve cada estado del reporte en pantalla
data class InfoEstado(
    val titulo: String,
    val color: Color,
    val detalle: String
)

// Los estados del reporte, en el orden en que avanza un reporte.
// La primera parte es el valor que guarda la base de datos; la segunda, cómo se ve en la app.
// (También es la lista de opciones del filtro del Home.)
val ESTADOS_REPORTE = listOf(
    "registrado" to "Registrado",
    "en_revision" to "En revisión",
    "en_seguimiento" to "En seguimiento",
    "canalizado" to "Canalizado",
    "concluido" to "Concluido",
    "archivado" to "Archivado",
    "cancelado" to "Cancelado",
    "reincidente" to "Reincidente",
    "pendiente_envio" to "Pendiente de envío"   // solo existe en el teléfono (sin internet)
)

// Convierte el estado que viene de la base de datos (columna "estado") en su texto y color
fun infoDeEstado(estado: String?): InfoEstado {
    return when (estado) {
        // Se guardó en el teléfono sin internet (no viene de la base de datos)
        "pendiente_envio" -> InfoEstado(
            titulo = "Pendiente de envío",
            color = GrisIcono,
            detalle = "Sin conexión. Se enviará automáticamente cuando haya internet."
        )

        // --- En proceso (morado) ---
        "en_revision" -> InfoEstado(
            titulo = "En revisión",
            color = Morado,
            detalle = "Estamos verificando la información de tu reporte."
        )
        "en_seguimiento" -> InfoEstado(
            titulo = "En seguimiento",
            color = Morado,
            detalle = "Tu reporte fue validado y se le da seguimiento."
        )

        // --- Turnado a otra autoridad (azul) ---
        "canalizado" -> InfoEstado(
            titulo = "Canalizado",
            color = AzulInstitucional,
            detalle = "Se turnó a la autoridad correspondiente."
        )

        // --- Resuelto (verde) ---
        "concluido" -> InfoEstado(
            titulo = "Concluido",
            color = VerdeConfirmado,
            detalle = "La atención de tu reporte terminó."
        )
        "reincidente" -> InfoEstado(
            titulo = "Reincidente",
            color = Magenta,
            detalle = "Ya había reportes de este caso; se suma a su seguimiento."
        )

        // --- Cerrados sin atención (gris y rojo) ---
        "archivado" -> InfoEstado(
            titulo = "Archivado",
            color = TextoSecundario,
            detalle = "No fue posible validar la información proporcionada."
        )
        "cancelado" -> InfoEstado(
            titulo = "Cancelado",
            color = RojoNoApto,
            detalle = "Este reporte fue cancelado."
        )

        // "registrado" (así empieza todo reporte) o cualquier estado nuevo. Pendiente (ámbar).
        else -> InfoEstado(
            titulo = "Registrado",
            color = AmarilloProgreso,
            detalle = "Recibimos tu reporte. Pronto empezaremos a revisarlo."
        )
    }
}
