package com.example.sipinnaapp.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sipinnaapp.R
import com.example.sipinnaapp.model.ReporteDetalle
import com.example.sipinnaapp.ui.theme.*
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Detalle de un reporte enviado, con lo que hay guardado en la base de datos
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaDetalleReporte(
    detalle: ReporteDetalle?,
    cargando: Boolean,
    mensajeError: String,
    alRegresar: () -> Unit,
    alReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SuperficiePrimaria)
    ) {
        // Encabezado: flecha + título
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = alRegresar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.Black)
            }
            Text(
                text = "Detalle del reporte",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(48.dp))
        }

        when {
            cargando -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator(color = Teal)
            }

            detalle == null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = mensajeError.ifEmpty { "No se pudo cargar el reporte." },
                    fontSize = 13.sp,
                    color = GrisTexto,
                    textAlign = TextAlign.Center
                )
                Espacio(12.dp)
                TextButton(onClick = alReintentar) {
                    Text("Reintentar", color = Teal, fontWeight = FontWeight.SemiBold)
                }
            }

            else -> ContenidoDetalle(detalle)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContenidoDetalle(detalle: ReporteDetalle) {
    val info = infoDeEstado(detalle.last_state)
    val partes = separarDescripcion(detalle.description)
    val tokenMapbox = stringResource(R.string.mapbox_access_token)
    val numFotos = detalle.images?.size ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Espacio(12.dp)

        // Estado actual
        TarjetaResumen {
            Text(info.titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = info.color)
            if (info.detalle.isNotEmpty()) {
                Espacio(4.dp)
                Text(info.detalle, fontSize = 12.sp, color = GrisTexto)
            }
            Espacio(6.dp)
            Text(
                text = "Última actualización: ${fechaLegible(detalle.state_changed_at)}",
                fontSize = 11.sp,
                color = GrisTextoClaro
            )
        }

        Espacio(12.dp)

        // Folio + fecha de envío
        TarjetaResumen {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    EtiquetaGris("Folio")
                    Text(detalle.folio ?: "—", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    EtiquetaGris("Enviado")
                    Text(fechaLegible(detalle.created_at), fontSize = 12.sp, color = GrisTexto)
                }
            }
            if (!detalle.sighting_time.isNullOrBlank()) {
                Espacio(10.dp)
                EtiquetaGris("Visto el")
                Text(fechaLegible(detalle.sighting_time), fontSize = 12.sp, color = Color.Black)
            }
        }

        Espacio(12.dp)

        // Ubicación con mapa
        TarjetaResumen {
            TituloSeccion(Icons.Default.Place, "Ubicación")
            Espacio(10.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Teal.copy(alpha = 0.15f))
            ) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Teal)
                if (detalle.latitude != null && detalle.longitude != null) {
                    AsyncImage(
                        model = urlMapa(detalle.latitude, detalle.longitude, tokenMapbox),
                        contentDescription = "Mapa del lugar del reporte",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Espacio(10.dp)
            Text(
                text = partes.direccion.ifBlank {
                    if (detalle.latitude != null && detalle.longitude != null)
                        "%.5f, %.5f".format(detalle.latitude, detalle.longitude)
                    else "Sin ubicación"
                },
                fontSize = 12.sp,
                color = Color.Black
            )
            if (!detalle.zone_name.isNullOrBlank()) {
                Espacio(4.dp)
                Text("Zona: ${detalle.zone_name}", fontSize = 11.sp, color = GrisTexto)
            }
        }

        Espacio(12.dp)

        // Fotos: por ahora solo cuántas hay (verlas requiere que el backend dé un enlace de S3)
        TarjetaResumen {
            TituloSeccion(Icons.Default.AccountBox, "Fotos del lugar")
            Espacio(8.dp)
            Text(
                text = when (numFotos) {
                    0 -> "Sin fotos"
                    1 -> "1 foto adjunta"
                    else -> "$numFotos fotos adjuntas"
                },
                fontSize = 12.sp,
                color = Color.Black
            )
        }

        Espacio(12.dp)

        // Descripción (sin las líneas de dirección y condición)
        TarjetaResumen {
            TituloSeccion(Icons.Default.Edit, "Descripción")
            Espacio(8.dp)
            Text(partes.texto.ifBlank { "—" }, fontSize = 12.sp, color = Color.Black)
        }

        Espacio(12.dp)

        // Información del niño
        TarjetaResumen {
            TituloSeccion(Icons.Default.Person, "Información del niño")
            if (!detalle.children_age.isNullOrBlank()) {
                Espacio(10.dp)
                EtiquetaGris("Edad aproximada")
                Espacio(4.dp)
                ChipResumen(detalle.children_age)
            }
            val tipos = detalle.work_type.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (tipos.isNotEmpty()) {
                Espacio(10.dp)
                EtiquetaGris("Tipo de trabajo")
                Espacio(4.dp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tipos.forEach { ChipResumen(it) }
                }
            }
            if (partes.condicion.isNotBlank()) {
                Espacio(10.dp)
                EtiquetaGris("Condición")
                Espacio(4.dp)
                ChipResumen(partes.condicion)
            }
        }

        Espacio(24.dp)
    }
}

// La app guarda la dirección y la condición al final de la descripción
// ("Dirección: ..." y "Condición: ..."). Aquí se vuelven a separar.
private data class PartesDescripcion(val texto: String, val direccion: String, val condicion: String)

private fun separarDescripcion(descripcion: String?): PartesDescripcion {
    var direccion = ""
    var condicion = ""
    val texto = mutableListOf<String>()
    descripcion.orEmpty().lines().forEach { linea ->
        when {
            linea.startsWith("Dirección:") -> direccion = linea.removePrefix("Dirección:").trim()
            linea.startsWith("Condición:") -> condicion = linea.removePrefix("Condición:").trim()
            else -> texto += linea
        }
    }
    return PartesDescripcion(texto.joinToString("\n").trim(), direccion, condicion)
}

// "2026-10-02T19:05:31.12Z" o "2026-10-02T19:00" → "2 oct 2026 · 13:05" (hora local)
private fun fechaLegible(valor: String?): String {
    if (valor.isNullOrBlank()) return "—"
    val formato = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale("es", "MX"))
    return try {
        OffsetDateTime.parse(valor).atZoneSameInstant(ZoneId.systemDefault()).format(formato)
    } catch (e: Exception) {
        try {
            LocalDateTime.parse(valor).format(formato)
        } catch (e2: Exception) {
            valor
        }
    }
}
