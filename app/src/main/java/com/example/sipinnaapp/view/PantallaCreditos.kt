package com.example.sipinnaapp.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipinnaapp.R
import com.example.sipinnaapp.ui.theme.*

// ---------- Datos de los créditos (editar aquí) ----------

data class Integrante(val nombre: String, val matricula: String = "")

val INTEGRANTES = listOf(
    Integrante("Emiliano Garcia Ramos", "A01753803"),
    Integrante("Jose Manuel  Bañuelos Silva", "A01804173"),
    Integrante("Alexis Maximiliano Alva Martínez", "A01754754"),
    Integrante("Néstor Daniel Huidobro Aguilar", "A01754717"),
    Integrante("Ángel Jesús Solorio García", "A01801489")
)

const val MATERIA = "Integración de seguridad informática en redes y sistemas de software"                      // ej. "TC2006B Interconexión de dispositivos"
const val PROFESOR = "…"                     // ej. "Mtro. Nombre Apellido"
const val INSTITUCION = "Tecnológico de Monterrey"
const val PERIODO = "AGO - DIC de 2026"                      // ej. "Agosto–Diciembre 2026"

// ---------------------------------------------------------

@Composable
fun PantallaCreditos(
    alRegresar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    // Versión de la app (la que dice build.gradle: versionName)
    val version = remember {
        runCatching {
            contexto.packageManager.getPackageInfo(contexto.packageName, 0).versionName
        }.getOrNull() ?: ""
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(FondoApp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = alRegresar, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.Black)
            }
        }

        Image(
            painter = painterResource(id = R.drawable.sipinna_logo),
            contentDescription = "Logo SIPINNA",
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(8.dp)
        )
        Espacio(12.dp)
        Text("SIPINNA", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        if (version.isNotBlank()) {
            Text("Versión $version", fontSize = 13.sp, color = GrisTexto)
        }
        Espacio(6.dp)
        Text(
            "App para reportar situaciones de trabajo infantil.",
            fontSize = 14.sp, color = GrisTexto, textAlign = TextAlign.Center
        )

        Espacio(28.dp)

        TarjetaCreditos(titulo = "Desarrollada por") {
            INTEGRANTES.forEach { integrante ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        integrante.nombre,
                        fontSize = 15.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    if (integrante.matricula.isNotBlank()) {
                        Text(integrante.matricula, fontSize = 13.sp, color = GrisTexto)
                    }
                }
            }
        }

        Espacio(14.dp)

        TarjetaCreditos(titulo = "Proyecto académico") {
            DatoCredito("Materia", MATERIA)
            DatoCredito("Profesor", PROFESOR)
            DatoCredito("Institución", INSTITUCION)
            DatoCredito("Periodo", PERIODO)
        }

        Espacio(24.dp)
        Text(
            "Hecho con Kotlin, Jetpack Compose y Mapbox.",
            fontSize = 12.sp, color = GrisTexto, textAlign = TextAlign.Center
        )
        Espacio(24.dp)
    }
}

@Composable
private fun TarjetaCreditos(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Teal, letterSpacing = 1.sp)
            Espacio(8.dp)
            contenido()
        }
    }
}

@Composable
private fun DatoCredito(etiqueta: String, valor: String) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(etiqueta, fontSize = 12.sp, color = GrisTexto)
        Text(valor, fontSize = 15.sp, color = Color.Black)
    }
}