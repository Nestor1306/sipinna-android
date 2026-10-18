package mx.tec.sipinna.view

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.tec.sipinna.ui.theme.Teal
import mx.tec.sipinna.ui.theme.TextoPrincipal

// Número de SIPINNITO (el asistente). Si el de WhatsApp es otro, se cambia solo aquí.
const val TELEFONO_SIPINNITO = "+17402848974"
const val WHATSAPP_SIPINNITO = "+17402848974"

// Mensaje con el que se abre el chat de WhatsApp
private const val SALUDO_WHATSAPP = "Hola SIPINNITO, quiero hacer un reporte."

private val VerdeWhatsApp = Color(0xFF25D366)

// Abre el marcador con el número escrito; la persona decide si llama (no pide permisos)
fun llamarASipinnito(contexto: Context) {
    contexto.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$TELEFONO_SIPINNITO")))
}

// Abre el chat de WhatsApp con SIPINNITO (si no hay WhatsApp, se abre en el navegador)
fun abrirWhatsAppSipinnito(contexto: Context) {
    val numero = WHATSAPP_SIPINNITO.filter { it.isDigit() }   // wa.me pide el número sin "+"
    val enlace = Uri.parse("https://wa.me/$numero?text=" + Uri.encode(SALUDO_WHATSAPP))
    try {
        contexto.startActivity(Intent(Intent.ACTION_VIEW, enlace))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(contexto, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
    }
}

// Los dos botones del asistente (WhatsApp y llamada), uno arriba del otro, solo con ícono
@Composable
fun BotonesAsistente(modifier: Modifier = Modifier) {
    val contexto = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        BotonAsistente(
            icono = Icons.AutoMirrored.Filled.Chat,
            fondo = VerdeWhatsApp,
            descripcion = "Escribir a SIPINNITO por WhatsApp",
            alPresionar = { abrirWhatsAppSipinnito(contexto) }
        )
        BotonAsistente(
            icono = Icons.Default.Call,
            fondo = Teal,
            descripcion = "Llamar a SIPINNITO",
            alPresionar = { llamarASipinnito(contexto) }
        )
    }
}

// Botón redondo con el ícono (la descripción la lee TalkBack)
@Composable
private fun BotonAsistente(
    icono: ImageVector,
    fondo: Color,
    descripcion: String,
    alPresionar: () -> Unit
) {
    FloatingActionButton(
        onClick = alPresionar,
        containerColor = fondo,
        contentColor = Color.White,
        shape = RoundedCornerShape(50),
        modifier = Modifier.size(48.dp)
    ) {
        Icon(icono, contentDescription = descripcion, modifier = Modifier.size(24.dp))
    }
}