package com.example.sipinnaapp.view

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
import com.example.sipinnaapp.ui.theme.Teal
import com.example.sipinnaapp.ui.theme.TextoPrincipal

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

// Los dos botones del asistente (WhatsApp y llamada), uno arriba del otro
@Composable
fun BotonesAsistente(modifier: Modifier = Modifier) {
    val contexto = LocalContext.current

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        BotonAsistente(
            texto = "WhatsApp",
            icono = Icons.AutoMirrored.Filled.Chat,
            fondo = VerdeWhatsApp,
            descripcion = "Escribir a SIPINNITO por WhatsApp",
            alPresionar = { abrirWhatsAppSipinnito(contexto) }
        )
        BotonAsistente(
            texto = "Llamar",
            icono = Icons.Default.Call,
            fondo = Teal,
            descripcion = "Llamar a SIPINNITO",
            alPresionar = { llamarASipinnito(contexto) }
        )
    }
}

// Etiqueta blanca a la izquierda + botón redondo con el ícono
@Composable
private fun BotonAsistente(
    texto: String,
    icono: ImageVector,
    fondo: Color,
    descripcion: String,
    alPresionar: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = alPresionar,
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Text(
                text = texto,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextoPrincipal,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        SmallFloatingActionButton(
            onClick = alPresionar,
            containerColor = fondo,
            contentColor = Color.White,
            shape = RoundedCornerShape(50)
        ) {
            Icon(icono, contentDescription = descripcion, modifier = Modifier.size(22.dp))
        }
    }
}
