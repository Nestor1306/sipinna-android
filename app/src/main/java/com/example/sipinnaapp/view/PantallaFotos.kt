package com.example.sipinnaapp.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sipinnaapp.ui.theme.*
import com.example.sipinnaapp.viewmodel.ReporteVM

@Composable
fun PantallaFotos(
    vm: ReporteVM,
    alRegresar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by vm.estado.collectAsState()

    // Abre la galería del teléfono y permite elegir varias fotos
    val elegirFotos = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(ReporteVM.MAX_FOTOS)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) vm.agregarFotos(uris.map { it.toString() })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        EncabezadoPaso(
            titulo = "Fotos del lugar",
            subtitulo = "Una foto del lugar del reporte nos ayudará a determinar la calidez de tu reporte y aportará a futuras contribuciones.",
            paso = 2,
            alRegresar = alRegresar
        )

        Espacio(20.dp)

        // Una fila de 2 cuadros: las fotos elegidas + el cuadro para agregar (máximo 2 fotos)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            estado.fotos.forEach { uri ->
                TarjetaFoto(uri = uri, alQuitar = { vm.quitarFoto(uri) }, modifier = Modifier.weight(1f))
            }
            if (estado.fotos.size < ReporteVM.MAX_FOTOS) {
                CuadroAgregarFoto(
                    alPresionar = {
                        elegirFotos.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            // Si no hay fotos, un espacio vacío para que el cuadro "+" no ocupe todo el ancho
            if (estado.fotos.isEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Espacio(8.dp)
        Text(
            text = "${estado.fotos.size} de ${ReporteVM.MAX_FOTOS} fotos (opcional)",
            fontSize = 11.sp,
            color = GrisTexto
        )

        Spacer(modifier = Modifier.weight(1f))

        BotonPrincipal(texto = "Siguiente", alPresionar = { vm.siguientePaso() })

        Espacio(24.dp)
    }
}

// Foto elegida con botón "x" para quitarla
@Composable
fun TarjetaFoto(uri: String, alQuitar: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "Foto del lugar",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = alQuitar)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Quitar", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

// Cuadro gris con ícono para agregar una foto
@Composable
fun CuadroAgregarFoto(alPresionar: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(SuperficieSecundaria)
            .clickable(onClick = alPresionar)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Agregar foto",
            tint = Color.Black,
            modifier = Modifier.size(48.dp)
        )
    }
}
