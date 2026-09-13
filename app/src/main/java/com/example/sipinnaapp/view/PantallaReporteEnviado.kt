package com.example.sipinnaapp.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sipinnaapp.ui.theme.*
import com.example.sipinnaapp.viewmodel.ReporteVM

@Composable
fun PantallaReporteEnviado(
    vm: ReporteVM,
    alCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by vm.estado.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SuperficiePrimaria)
            .padding(horizontal = 18.dp)
    ) {
        // X para cerrar e ícono de info
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = alCerrar, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Black, modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = { /* pendiente: info */ }) {
                Icon(Icons.Default.Info, contentDescription = "Información", tint = Color.Black, modifier = Modifier.size(28.dp))
            }
        }

        Text(
            text = "Reporte confirmado",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = VerdeConfirmado
        )

        Espacio(6.dp)

        Text(
            text = "Folio: ${estado.folioGenerado}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Espacio(32.dp)

        // Tarjeta de agradecimiento
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "¡Gracias por contribuir!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                if (estado.fotos.isNotEmpty()) {
                    Espacio(16.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        estado.fotos.forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }

                Espacio(16.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .padding(12.dp)
                ) {
                    Text(estado.descripcion, fontSize = 13.sp, color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        BotonPrincipal(texto = "Volver al inicio", alPresionar = alCerrar)

        Espacio(24.dp)
    }
}
