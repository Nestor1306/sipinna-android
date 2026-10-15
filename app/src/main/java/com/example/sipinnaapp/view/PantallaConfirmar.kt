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
import androidx.compose.foundation.clickable
import java.util.Calendar
import java.util.TimeZone
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaConfirmar(
    vm: ReporteVM,
    alRegresar: () -> Unit,
    alEnviar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by vm.estado.collectAsState()
    val formatoFecha = remember { SimpleDateFormat("d MMM yyyy · h:mm a", Locale("es", "MX")) }
    val fecha = formatoFecha.format(Date(estado.fechaAvistamiento ?: System.currentTimeMillis()))
    var mostrarSelectorFecha by remember { mutableStateOf(false) }

    if (mostrarSelectorFecha) {
        SelectorFechaHora(
            fechaActual = estado.fechaAvistamiento ?: System.currentTimeMillis(),
            alElegir = {
                vm.cambiarFechaAvistamiento(it)
                mostrarSelectorFecha = false
            },
            alCerrar = { mostrarSelectorFecha = false }
        )
    }
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
                    // Se toca para cambiar cuándo se vio la situación
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = !estado.cargando) { mostrarSelectorFecha = true }
                            .padding(4.dp)
                    ) {
                        EtiquetaGris("Fecha y hora")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(fecha, fontSize = 12.sp, color = Color.Black)
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Cambiar fecha y hora",
                                tint = Teal,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Espacio(12.dp)

            // Ubicación
            TarjetaResumen {
                TituloSeccion(Icons.Default.Place, "Ubicación", alEditar = { if (!estado.cargando) vm.editarDesdeConfirmar(1) })
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
                TituloSeccion(Icons.Default.AccountBox, "Fotos del lugar", alEditar = { if (!estado.cargando) vm.editarDesdeConfirmar(2) })
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
                TituloSeccion(Icons.Default.Edit, "Descripción", alEditar = { if (!estado.cargando) vm.editarDesdeConfirmar(4) })
                Espacio(8.dp)
                Text(estado.descripcion, fontSize = 12.sp, color = Color.Black)
            }

            Espacio(12.dp)

            // Información del niño
            TarjetaResumen {
                TituloSeccion(Icons.Default.Person, "Información del Niño/Niña/Adolescente", alEditar = { if (!estado.cargando) vm.editarDesdeConfirmar(3) })
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(color = Teal)
                    if (estado.progresoEnvio.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(estado.progresoEnvio, fontSize = 12.sp, color = GrisTexto)
                    }
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
fun TituloSeccion(icono: ImageVector, texto: String, alEditar: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (alEditar != null) Modifier.fillMaxWidth() else Modifier
    ) {
        Icon(icono, contentDescription = null, tint = Teal, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = texto.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            letterSpacing = 1.sp,
            modifier = if (alEditar != null) Modifier.weight(1f) else Modifier
        )
        // Solo en la Confirmación: lleva al paso de esta sección
        if (alEditar != null) {
            TextButton(
                onClick = alEditar,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Editar", fontSize = 11.sp, color = Teal, fontWeight = FontWeight.SemiBold)
            }
        }
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

// Calendario y luego reloj para elegir cuándo se vio la situación.
// No deja elegir días futuros; si la hora queda en el futuro, el ViewModel lo rechaza.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFechaHora(
    fechaActual: Long,
    alElegir: (Long) -> Unit,
    alCerrar: () -> Unit
) {
    var eligiendoHora by remember { mutableStateOf(false) }
    val actual = remember { Calendar.getInstance().apply { timeInMillis = fechaActual } }

    // El calendario de Material trabaja en UTC: el día local se pasa como medianoche UTC
    val estadoFecha = rememberDatePickerState(
        initialSelectedDateMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(actual.get(Calendar.YEAR), actual.get(Calendar.MONTH), actual.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                utcTimeMillis <= System.currentTimeMillis()
        }
    )
    val estadoHora = rememberTimePickerState(
        initialHour = actual.get(Calendar.HOUR_OF_DAY),
        initialMinute = actual.get(Calendar.MINUTE),
        is24Hour = false
    )

    if (!eligiendoHora) {
        DatePickerDialog(
            onDismissRequest = alCerrar,
            confirmButton = {
                TextButton(
                    onClick = { eligiendoHora = true },
                    enabled = estadoFecha.selectedDateMillis != null
                ) { Text("Siguiente", color = Teal) }
            },
            dismissButton = {
                TextButton(onClick = alCerrar) { Text("Cancelar", color = GrisTexto) }
            }
        ) {
            DatePicker(state = estadoFecha)
        }
    } else {
        AlertDialog(
            onDismissRequest = alCerrar,
            title = { Text("¿A qué hora lo viste?", fontSize = 16.sp) },
            text = { TimePicker(state = estadoHora) },
            confirmButton = {
                TextButton(onClick = {
                    // Día elegido (UTC) + hora elegida (local) → momento en hora local
                    val dia = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = estadoFecha.selectedDateMillis ?: fechaActual
                    }
                    val elegido = Calendar.getInstance().apply {
                        clear()
                        set(
                            dia.get(Calendar.YEAR), dia.get(Calendar.MONTH), dia.get(Calendar.DAY_OF_MONTH),
                            estadoHora.hour, estadoHora.minute
                        )
                    }
                    alElegir(elegido.timeInMillis)
                }) { Text("Aceptar", color = Teal) }
            },
            dismissButton = {
                TextButton(onClick = { eligiendoHora = false }) { Text("Atrás", color = GrisTexto) }
            }
        )
    }
}
