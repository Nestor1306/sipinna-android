package com.example.sipinnaapp.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipinnaapp.R

@Composable
fun PantallaHome(
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Espacio(48.dp)

        Image(
            painter = painterResource(id = R.drawable.sipinna_logo),
            contentDescription = "Logo SIPINNA",
            modifier = Modifier.size(100.dp)
        )

        Espacio(24.dp)

        Text(
            text = "¿Qué quieres hacer?",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Espacio(32.dp)

        Button(
            onClick = { /* pendiente: flujo de reporte */ },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Hacer un reporte", fontSize = 16.sp)
        }

        Espacio(12.dp)

        OutlinedButton(
            onClick = { /* pendiente: historial */ },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Ver mis reportes", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = alCerrarSesion) {
            Text("Cerrar sesión")
        }
    }
}
