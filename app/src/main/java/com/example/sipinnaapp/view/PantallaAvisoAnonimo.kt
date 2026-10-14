package com.example.sipinnaapp.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Rojo más oscuro que RojoNoApto para que el texto blanco se lea bien
private val RojoAviso = Color(0xFFC62828)

// Aviso antes de entrar sin cuenta: sin registrarse no se puede dar seguimiento al reporte
@Composable
fun PantallaAvisoAnonimo(
    alContinuarAnonimo: () -> Unit,
    alCrearCuenta: () -> Unit,
    alRegresar: () -> Unit,
    modifier: Modifier = Modifier
) {
    // El botón "atrás" del teléfono regresa al inicio de sesión
    BackHandler(onBack = alRegresar)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(RojoAviso)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp)
    ) {
        Espacio(40.dp)

        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(72.dp)
        )

        Espacio(16.dp)

        Text(
            text = "Antes de continuar",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Espacio(16.dp)

        Text(
            text = "Si entras sin cuenta, tus reportes sí llegarán a las autoridades, pero no podrás darles seguimiento:",
            fontSize = 15.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Espacio(16.dp)

        // Lo que no podrá hacer
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                "No verás el estado de tus reportes.",
                "No tendrás un folio para consultarlos.",
                "No recibirás avisos sobre su avance."
            ).forEach { punto ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("•  ", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(punto, fontSize = 15.sp, color = Color.White)
                }
            }
        }

        Espacio(16.dp)

        Text(
            text = "Si quieres saber qué pasa con tus reportes, crea una cuenta.",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Espacio(32.dp)

        // Opción recomendada: botón blanco
        Button(
            onClick = alCrearCuenta,
            shape = RoundedCornerShape(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = RojoAviso),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Crear una cuenta", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }

        Espacio(12.dp)

        // Seguir anónimo: solo con borde, para que no sea la opción principal
        OutlinedButton(
            onClick = alContinuarAnonimo,
            shape = RoundedCornerShape(60.dp),
            border = BorderStroke(1.5.dp, Color.White),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Entendido, continuar sin cuenta", fontSize = 15.sp)
        }

        Espacio(8.dp)

        TextButton(onClick = alRegresar) {
            Text("Regresar", color = Color.White.copy(alpha = 0.85f))
        }

        Espacio(16.dp)
    }
}
