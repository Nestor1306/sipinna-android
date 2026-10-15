package com.example.sipinnaapp.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sipinnaapp.R
import com.example.sipinnaapp.model.ReporteResumen
import com.example.sipinnaapp.ui.theme.*
import android.widget.Toast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHome(
    reportes: List<ReporteResumen>,
    cargando: Boolean,
    mensajeError: String,
    esAnonimo: Boolean,
    alHacerReporte: () -> Unit,
    alIrAPerfil: () -> Unit,
    alAbrirReporte: (ReporteResumen) -> Unit,
    alRefrescar: () -> Unit,
    avisoSinConexion: String,       // "" = hay conexión; si no, se muestra arriba de las tarjetas
    modifier: Modifier = Modifier
) {
    var busqueda by remember { mutableStateOf("") }
    var filtroEstado by remember { mutableStateOf<String?>(null) }   // null = todos
    var masAntiguosPrimero by remember { mutableStateOf(false) }
    var menuFiltroAbierto by remember { mutableStateOf(false) }
    // Solo para el gesto de "jalar para actualizar" (la actualización automática no muestra el círculo)
    var refrescando by remember { mutableStateOf(false) }
    LaunchedEffect(cargando) { if (!cargando) refrescando = false }


    // La lista ya llega de más reciente a más antiguo (pendientes arriba, luego por folio).
    // Aquí se aplica el buscador, el filtro de estado y el orden elegido.
    val reportesFiltrados = reportes
        .filter {
            busqueda.isBlank() ||
                    (it.description ?: "").contains(busqueda, ignoreCase = true) ||
                    (it.folio ?: "").contains(busqueda, ignoreCase = true)
        }
        .filter { filtroEstado == null || it.report_state == filtroEstado }
        .let { if (masAntiguosPrimero) it.reversed() else it }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FondoApp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Espacio(14.dp)

            // Logo a la izquierda, perfil a la derecha
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.sipinna_logo),
                    contentDescription = "Logo SIPINNA",
                    modifier = Modifier.height(38.dp)
                )
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Mi perfil",
                    tint = GrisIcono,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = alIrAPerfil)
                )
            }

            Espacio(28.dp)

            // Buscador + botón de filtro
            if (!esAnonimo){
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 5.dp)
            ) {
                CampoBusqueda(
                    valor = busqueda,
                    alCambiar = { busqueda = it },
                    placeholder = "Busca por folio o descripción",
                    modifier = Modifier.weight(1f)
                )
                // Botón de filtro con su menú
                Box {
                    BotonFiltro(
                        alPresionar = { menuFiltroAbierto = true },
                        activo = filtroEstado != null || masAntiguosPrimero
                    )
                    DropdownMenu(
                        expanded = menuFiltroAbierto,
                        onDismissRequest = { menuFiltroAbierto = false }
                    ) {
                        Text(
                            "Estado",
                            fontSize = 12.sp,
                            color = GrisTexto,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        (listOf(null to "Todos") + ESTADOS_REPORTE).forEach { (valor, etiqueta) ->
                            DropdownMenuItem(
                                text = { Text(etiqueta) },
                                leadingIcon = {
                                    if (filtroEstado == valor) Icon(Icons.Default.Check, contentDescription = null, tint = Teal)
                                },
                                onClick = {
                                    filtroEstado = valor
                                    menuFiltroAbierto = false
                                }
                            )
                        }
                        HorizontalDivider()
                        Text(
                            "Orden",
                            fontSize = 12.sp,
                            color = GrisTexto,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        listOf(false to "Más recientes primero", true to "Más antiguos primero").forEach { (antiguos, etiqueta) ->
                            DropdownMenuItem(
                                text = { Text(etiqueta) },
                                leadingIcon = {
                                    if (masAntiguosPrimero == antiguos) Icon(Icons.Default.Check, contentDescription = null, tint = Teal)
                                },
                                onClick = {
                                    masAntiguosPrimero = antiguos
                                    menuFiltroAbierto = false
                                }
                            )
                        }
                    }
                }
            }
            }

            Espacio(if (avisoSinConexion.isEmpty()) 24.dp else 12.dp)

            // Aviso cuando lo que se ve es la copia guardada en el teléfono
            if (avisoSinConexion.isNotEmpty()) {
                Text(
                    text = avisoSinConexion,
                    fontSize = 12.sp,
                    color = GrisTexto,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SuperficieSecundaria)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Espacio(12.dp)
            }

            // Tarjetas de reportes
            if (cargando && reportes.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CircularProgressIndicator(color = Teal)
                }
            } else if (reportesFiltrados.isEmpty()) {
                // Pantalla vacía como en el Figma: logo al centro y un mensaje abajo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 106.dp)   // compensa el encabezado para quedar al centro de la pantalla
                ) {
                Image(
                        painter = painterResource(id = R.drawable.sipinna_logo),
                        contentDescription = null,
                        modifier = Modifier.size(90.dp)
                    )
                    Espacio(12.dp)
                    Text(
                        text = when {
                            // El estado de los reportes solo lo ven los usuarios registrados
                            esAnonimo -> "Estás sin registrarte.\nCrea una cuenta para ver el estado de tus reportes."
                            mensajeError.isNotEmpty() -> mensajeError
                            reportes.isEmpty() -> "Aún no tienes ningún reporte.\nToca + para crear el primero."
                            else -> "No hay reportes que coincidan."
                        },
                        fontSize = 13.sp,
                        color = GrisTexto,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = refrescando,
                    onRefresh = {
                        refrescando = true
                        alRefrescar()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(15.dp),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(reportesFiltrados) { reporte ->
                            TarjetaReporte(reporte, alTocar = { alAbrirReporte(reporte) })
                        }
                    }
                }
            }
        }

        // Botón para hacer un reporte nuevo (abajo a la derecha), con texto para que se entienda
        ExtendedFloatingActionButton(
            onClick = alHacerReporte,
            containerColor = AzulBoton,      // o Teal, para que combine con el resto de la app
            contentColor = Color.White,
            shape = RoundedCornerShape(32.dp),
            icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(26.dp)) },
            text = { Text("Hacer reporte", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
        )
    }
}

// Pastilla gris con el ícono de filtro (a la derecha del buscador)
@Composable
fun BotonFiltro(alPresionar: () -> Unit, activo: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(74.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(if (activo) Teal else SuperficieSecundaria)
            .clickable(onClick = alPresionar)
    ) {
        Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = if (activo) "Filtrar (hay un filtro activo)" else "Filtrar",
            tint = if (activo) Color.White else Color.Black,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Corte diagonal del mapa dentro de la tarjeta (como en Figma)
private val FormaDiagonal = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width * 0.62f, size.height)
    lineTo(0f, size.height)
    close()
}

// Dirección de una imagen fija del mapa (Mapbox Static Images) con un pin en el reporte.
// OJO: Mapbox pide primero la longitud y después la latitud.
// 330x170 @2x = el tamaño del mapa en la tarjeta (165 x 85 dp) en buena resolución.
fun urlMapa(latitud: Double, longitud: Double, token: String): String {
    return "https://api.mapbox.com/styles/v1/mapbox/streets-v12/static/" +
        "pin-s+0a8f91($longitud,$latitud)/$longitud,$latitud,15,0/330x170@2x" +
        "?access_token=$token"
}

// Tarjeta de un reporte: mapa a la izquierda, estado y detalle a la derecha
@Composable
fun TarjetaReporte(reporte: ReporteResumen, alTocar: () -> Unit) {
    // Texto y color según el estado real guardado en la base de datos
    val info = infoDeEstado(reporte.report_state)
    val titulo = info.titulo
    val colorTitulo = info.color
    val detalle = info.detalle

    // La misma clave de Mapbox que usa el mapa de la pantalla de ubicación
    val tokenMapbox = stringResource(R.string.mapbox_access_token)

    Card(
        onClick = alTocar, // abre el detalle del reporte
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Lado izquierdo: mapa del lugar del reporte, con el corte diagonal
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(165.dp)
                    .fillMaxHeight()
                    .clip(FormaDiagonal)
                    .background(Teal.copy(alpha = 0.15f))
            ) {
                // Primero el pin sobre el fondo. Si el mapa carga, queda encima y lo tapa;
                // si no carga (sin internet, o la clave de Mapbox no lo permite), se ve el pin.
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier
                        .size(24.dp)
                        .offset(x = (-14).dp)
                )
                if (reporte.latitude != null && reporte.longitude != null) {
                    AsyncImage(
                        model = urlMapa(reporte.latitude, reporte.longitude, tokenMapbox),
                        contentDescription = "Mapa del lugar del reporte",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Lado derecho: estado + descripción
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 11.dp, end = 12.dp)
                    .width(188.dp)
            ) {
                Text(
                    text = titulo,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorTitulo,
                    textAlign = TextAlign.End
                )
                if (detalle.isNotEmpty()) {
                    Text(
                        text = detalle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = GrisTextoClaro,
                        textAlign = TextAlign.End,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}


// Folio con un botón para copiarlo (para buscarlo después o compartirlo)
@Composable
fun FolioCopiable(folio: String, tamano: TextUnit = 12.sp) {
    val portapapeles = LocalClipboardManager.current
    val contexto = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable {
                portapapeles.setText(AnnotatedString(folio))
                Toast.makeText(contexto, "Folio copiado", Toast.LENGTH_SHORT).show()
            }
            .padding(vertical = 2.dp)
    ) {
        Text(folio, fontSize = tamano, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar folio", tint = Teal, modifier = Modifier.size(16.dp))
    }
}
