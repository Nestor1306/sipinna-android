package com.example.sipinnaapp.view

import androidx.compose.ui.graphics.Color
import com.example.sipinnaapp.ui.theme.AmarilloProgreso
import com.example.sipinnaapp.ui.theme.AzulInstitucional
import com.example.sipinnaapp.ui.theme.GrisIcono
import com.example.sipinnaapp.ui.theme.Morado
import com.example.sipinnaapp.ui.theme.VerdeConfirmado

// Los colores de cada estado son los mismos que usa la web (dashboard de www.sipinna.com)

// Cómo se ve cada estado del reporte en pantalla
data class InfoEstado(
    val titulo: String,
    val color: Color,
    val detalle: String
)

// Estados que el backend le muestra al ciudadano (ya traducidos), en el orden en que
// avanza un reporte. La primera parte es el valor que llega de la API; la segunda, cómo
// se ve en la app. También es la lista de opciones del filtro del Home.
val ESTADOS_REPORTE = listOf(
    "recibido" to "Recibido",
    "en_revision" to "En revisión",
    "canalizado" to "Canalizado",
    "en_atencion" to "En atención",
    "concluido" to "Concluido",
    "pendiente_envio" to "Pendiente de envío"   // solo existe en el teléfono (sin internet)
)

// El backend ya manda el estado público, pero los datos guardados en el teléfono
// (historial sin conexión, estados conocidos de las notificaciones) pueden traer los
// nombres internos de antes. Esta función los traduce igual que el backend.
fun estadoPublico(estado: String?): String = when (estado) {
    null, "" -> "recibido"
    "DRAFT", "borrador" -> "borrador"
    "pendiente_envio" -> "pendiente_envio"
    "registrado", "recibido" -> "recibido"
    "canalizado" -> "canalizado"
    "en_atencion", "en_seguimiento" -> "en_atencion"
    "concluido", "archivado", "cancelado" -> "concluido"
    // en_revision, validado, caso_confirmado, reincidente, duplicado y cualquier estado nuevo
    else -> "en_revision"
}

// Convierte el estado (nuevo o viejo) en su texto y color
fun infoDeEstado(estado: String?): InfoEstado {
    return when (estadoPublico(estado)) {
        // Se guardó en el teléfono sin internet (no viene de la base de datos)
        "pendiente_envio" -> InfoEstado(
            titulo = "Pendiente de envío",
            color = GrisIcono,
            detalle = "Sin conexión. Se enviará automáticamente cuando haya internet."
        )

        // --- Pendiente (ámbar) ---
        "recibido" -> InfoEstado(
            titulo = "Recibido",
            color = AmarilloProgreso,
            detalle = "Recibimos tu reporte. Pronto empezaremos a revisarlo."
        )

        // --- En proceso (morado) ---
        "en_revision" -> InfoEstado(
            titulo = "En revisión",
            color = Morado,
            detalle = "Estamos verificando la información de tu reporte."
        )
        "en_atencion" -> InfoEstado(
            titulo = "En atención",
            color = Morado,
            detalle = "Tu reporte está siendo atendido."
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
            detalle = "La atención de este reporte terminó."
        )

        // "borrador" (no debería mostrarse: se filtra antes)
        else -> InfoEstado(
            titulo = "Borrador",
            color = GrisIcono,
            detalle = "Este reporte no se terminó de enviar."
        )
    }
}