package com.example.sipinnaapp.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.sipinnaapp.ui.theme.*
import com.example.sipinnaapp.viewmodel.ReporteVM
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

// MAPBOX
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.Marker

import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@OptIn(MapboxExperimental::class)
@Composable
fun PantallaLocacion(
    vm: ReporteVM,
    alRegresar: () -> Unit,
    modifier: Modifier = Modifier
) {

    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current
    val enfoque = LocalFocusManager.current   // para esconder el teclado al elegir un lugar

    // Cámara inicial del mapa.
    // Mientras todavía no tenemos GPS, muestra Ciudad de México.
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(
                Point.fromLngLat(
                    -99.1332,
                    19.4326
                )
            )
            zoom(11.0)
        }
    }

    // Convierte coordenadas a una dirección escrita.
    fun obtenerDireccion(
        latitud: Double,
        longitud: Double
    ) {

        try {

            val direcciones =
                Geocoder(
                    contexto,
                    Locale.getDefault()
                ).getFromLocation(
                    latitud,
                    longitud,
                    1
                )

            val primera =
                direcciones
                    ?.firstOrNull()
                    ?.getAddressLine(0)

            if (!primera.isNullOrEmpty()) {
                vm.actualizarDireccion(primera)
            }

        } catch (_: Exception) {
            // Si falla el Geocoder,
            // las coordenadas se conservan.
        }
    }

    // Cuando cambian las coordenadas,
    // mueve automáticamente la cámara al punto seleccionado.
    LaunchedEffect(
        estado.latitud,
        estado.longitud
    ) {

        if (
            estado.latitud != 0.0 &&
            estado.longitud != 0.0
        ) {

            val punto = Point.fromLngLat(
                estado.longitud,
                estado.latitud
            )

            mapViewportState.setCameraOptions(
                CameraOptions.Builder()
                    .center(punto)
                    .zoom(17.0)
                    .build()
            )
        }
    }

    // Obtiene la ubicación GPS del dispositivo.
    @SuppressLint("MissingPermission")
    fun obtenerUbicacion() {

        vm.actualizarBuscandoUbicacion(true)

        val cliente =
            LocationServices.getFusedLocationProviderClient(
                contexto
            )

        cliente.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        )
            .addOnSuccessListener { ubicacion ->

                if (ubicacion != null) {

                    // Guarda GPS en el ViewModel
                    vm.actualizarCoordenadas(
                        ubicacion.latitude,
                        ubicacion.longitude
                    )

                    // Busca la dirección correspondiente
                    obtenerDireccion(
                        ubicacion.latitude,
                        ubicacion.longitude
                    )

                } else {

                    vm.actualizarBuscandoUbicacion(false)
                }
            }
            .addOnFailureListener {

                vm.actualizarBuscandoUbicacion(false)
            }
    }

    // Pide permiso de ubicación.
    val pedirPermiso =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { concedido ->

            if (concedido) {
                obtenerUbicacion()
            }
        }

    fun alPresionarMiUbicacion() {

        val yaTienePermiso =
            ContextCompat.checkSelfPermission(
                contexto,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (yaTienePermiso) {

            obtenerUbicacion()

        } else {

            pedirPermiso.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    // Obtiene automáticamente la ubicación
    // cuando se abre esta pantalla.
    LaunchedEffect(Unit) {
        alPresionarMiUbicacion()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {

        EncabezadoPaso(
            titulo = "Ubicación del reporte",
            subtitulo =
                "Indícanos el lugar exacto del reporte. Puedes tocar el mapa o usar tu ubicación actual.",
            paso = 1,
            alRegresar = alRegresar
        )

        Espacio(16.dp)

        AvisoEmergencia()

        Espacio(16.dp)

        CampoBusqueda(
            valor = estado.direccion,
            alCambiar = {
                vm.actualizarDireccion(it)
                vm.buscarLugares(it)
            },
            placeholder = "Busca una calle, colonia o lugar"
        )

        Espacio(20.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(11.dp))
        ) {

            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,

                // NUEVO:
                // Cuando el usuario toca el mapa,
                // mueve el pin y guarda las coordenadas.
                onMapClickListener = { punto ->

                    val latitudNueva =
                        punto.latitude()

                    val longitudNueva =
                        punto.longitude()

                    vm.cerrarSugerencias()

                    vm.actualizarCoordenadas(
                        latitudNueva,
                        longitudNueva
                    )

                    // Cambia el texto de la dirección
                    // según el lugar seleccionado.
                    obtenerDireccion(
                        latitudNueva,
                        longitudNueva
                    )

                    true
                }
            ) {

                // PIN
                if (
                    estado.latitud != 0.0 &&
                    estado.longitud != 0.0
                ) {

                    Marker(
                        point = Point.fromLngLat(
                            estado.longitud,
                            estado.latitud
                        ),
                        color = Teal,
                        innerColor = Color.White
                    )
                }
            }

            // Botón para regresar a la ubicación GPS actual.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(45.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
            ) {

                if (estado.buscandoUbicacion) {

                    CircularProgressIndicator(
                        color = Teal,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )

                } else {

                    IconButton(
                        onClick = {
                            alPresionarMiUbicacion()
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.LocationOn,
                            contentDescription =
                                "Usar mi ubicación",
                            tint = Teal
                        )
                    }
                }
            }
            // Lista de lugares sugeridos, encima del mapa
            if (estado.sugerencias.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column {
                        estado.sugerencias.forEachIndexed { i, lugar ->
                            if (i > 0) HorizontalDivider(color = SuperficieSecundaria)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        enfoque.clearFocus()
                                        vm.elegirLugar(lugar)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    tint = Teal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = lugar.nombre,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (lugar.direccion.isNotBlank() && lugar.direccion != lugar.nombre) {
                                        Text(
                                            text = lugar.direccion,
                                            fontSize = 11.sp,
                                            color = GrisTexto,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Espacio(20.dp)

        BotonPrincipal(
            texto = if (estado.editandoDesdeConfirmar) "Guardar cambios" else "Confirmar",
            alPresionar = {
                vm.siguientePaso()
            },
            habilitado = ReporteVM.errorDelPaso(estado).isEmpty()
        )

        Espacio(24.dp)
    }
}