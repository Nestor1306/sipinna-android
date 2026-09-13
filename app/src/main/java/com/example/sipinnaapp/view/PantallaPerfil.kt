package com.example.sipinnaapp.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipinnaapp.ui.theme.*

@Composable
fun PantallaPerfil(
    nombre: String,
    alRegresar: () -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(FondoApp)
            .padding(horizontal = 18.dp)
    ) {
        // Flecha de regreso
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = alRegresar, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar",
                    tint = Color.Black
                )
            }
        }

        Espacio(8.dp)

        // Foto de perfil (círculo grande con borde)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(175.dp)
                .clip(CircleShape)
                .background(SuperficieSecundaria)
                .border(1.dp, Color.Black, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = GrisIcono,
                modifier = Modifier.size(90.dp)
            )
        }

        Espacio(28.dp)

        // Nombre + lápiz para editar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = nombre.ifBlank { "Ciudadano" },
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(onClick = { /* pendiente: editar nombre */ }, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar nombre",
                    tint = AzulEditar,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Espacio(6.dp)

        Text(
            text = "Este nombre se vinculará con tus reportes.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = GrisTextoClaro,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(188.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = alCerrarSesion) {
            Text("Cerrar sesión", color = RojoNoApto, fontSize = 15.sp)
        }

        Espacio(24.dp)
    }
}
