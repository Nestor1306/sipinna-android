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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sipinnaapp.ui.theme.*
import com.example.sipinnaapp.viewmodel.ReporteVM
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaConfirmar(
    vm: ReporteVM,
    alRegresar: () -> Unit,
    alEnviar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by vm.estado.collectAsState()
    val fecha = remember { SimpleDateFormat("d MMM yyyy · HH:mm", Locale("es", "MX")).format(Date()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SuperficiePrimaria)
    ) {
        // Encabezado: flecha + título centrado
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
                text = "Confirmación del reporte",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(48.dp))
        }
        Box(Modifier.padding(horizontal = 18.dp)) { BarraProgreso(paso = 5, total = ReporteVM.TOTAL_PASOS) }

        // Contenido con scroll
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            Espacio(20.dp)

            // Folio + fecha
            TarjetaResumen {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        EtiquetaGris("Folio")
                        Text("Se asigna al enviar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        EtiquetaGris("Fecha")
                        Text(fecha, fontSize = 12.sp, color = GrisTexto)
                    }
                }
            }

            Espacio(12.dp)

            // Ubicación
            TarjetaResumen {
                TituloSeccion(Icons.Default.Place, "Ubicación")
                Espacio(10.dp)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Teal.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Teal)
                }
                Espacio(10.dp)
                Text(
                    text = estado.direccion.ifBlank { "%.5f, %.5f".format(estado.latitud, estado.longitud) },
                    fontSize = 12.sp,
                    color = Color.Black
                )
            }

            Espacio(12.dp)

            // Fotos
            TarjetaResumen {
                TituloSeccion(Icons.Default.AccountBox, "Fotos del lugar")
                Espacio(10.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    estado.fotos.forEach { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SuperficieSecundaria)
                    ) {
                        Text(
                            text = "${estado.fotos.size} foto${if (estado.fotos.size == 1) "" else "s"}",
                            fontSize = 10.sp,
                            color = GrisTexto
                        )
                    }
                }
            }

            Espacio(12.dp)

            // Descripción
            TarjetaResumen {
                TituloSeccion(Icons.Default.Edit, "Descripción")
                Espacio(8.dp)
                Text(estado.descripcion, fontSize = 12.sp, color = Color.Black)
            }

            Espacio(12.dp)

            // Información del niño
            TarjetaResumen {
                TituloSeccion(Icons.Default.Person, "Información del niño")
                Espacio(10.dp)
                EtiquetaGris("Edad aproximada")
                Espacio(4.dp)
                ChipResumen(estado.edadNino)
                Espacio(10.dp)
                EtiquetaGris("Tipo de trabajo")
                Espacio(4.dp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    estado.tiposTrabajo.forEach { ChipResumen(it) }
                }
                Espacio(10.dp)
                EtiquetaGris("Condición")
                Espacio(4.dp)
                ChipResumen(estado.condicion)
            }

            Espacio(16.dp)

            // ¿Algo incorrecto? Editar reporte
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("¿Algo incorrecto? ", fontSize = 11.sp, color = GrisTexto)
                TextButton(onClick = { vm.irAPaso(1) }, contentPadding = PaddingValues(0.dp)) {
                    Text("Editar reporte", fontSize = 11.sp, color = Teal, fontWeight = FontWeight.SemiBold)
                }
            }

            Espacio(8.dp)
        }

        // Botón enviar (fijo abajo)
        Box(Modifier.padding(horizontal = 18.dp)) {
            if (estado.cargando) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(46.dp)) {
                    CircularProgressIndicator(color = Teal)
                }
            } else {
                BotonPrincipal(texto = "Enviar reporte", alPresionar = alEnviar)
            }
        }
        Espacio(24.dp)
    }
}

// --- Piezas de la pantalla de confirmación ---

@Composable
fun TarjetaResumen(contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = contenido)
    }
}

@Composable
fun TituloSeccion(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = Teal, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = texto.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun EtiquetaGris(texto: String) {
    Text(texto.uppercase(), fontSize = 9.sp, color = GrisTexto, letterSpacing = 0.5.sp)
}

@Composable
fun ChipResumen(texto: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Teal.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(texto, fontSize = 11.sp, color = TealOscuro, fontWeight = FontWeight.Medium)
    }
}
