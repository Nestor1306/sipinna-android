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

@OptIn(MapboxExperimental::class)
@Composable
fun PantallaLocacion(
    vm: ReporteVM,
    alRegresar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current

    // Estado de la cámara del mapa.
    // Si todavía no tenemos GPS, inicia mostrando Ciudad de México.
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(-99.1332, 19.4326))
            zoom(11.0)
        }
    }

    // Cuando obtenemos las coordenadas GPS,
    // mueve automáticamente el mapa hacia esa ubicación.
    LaunchedEffect(estado.latitud, estado.longitud) {
        if (estado.latitud != 0.0 && estado.longitud != 0.0) {
            val punto = Point.fromLngLat(
                estado.longitud,
                estado.latitud
            )

            mapViewportState.setCameraOptions(
                CameraOptions.Builder()
                    .center(punto)
                    .zoom(16.0)
                    .build()
            )
        }
    }

    // Obtiene la ubicación actual con GPS y la convierte en dirección.
    @SuppressLint("MissingPermission")
    fun obtenerUbicacion() {
        vm.actualizarBuscandoUbicacion(true)

        val cliente =
            LocationServices.getFusedLocationProviderClient(contexto)

        cliente.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        )
            .addOnSuccessListener { ubicacion ->

                if (ubicacion != null) {

                    vm.actualizarCoordenadas(
                        ubicacion.latitude,
                        ubicacion.longitude
                    )

                    // Convierte latitud/longitud en una dirección escrita.
                    try {
                        val direcciones =
                            Geocoder(
                                contexto,
                                Locale.getDefault()
                            ).getFromLocation(
                                ubicacion.latitude,
                                ubicacion.longitude,
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
                        // Si el Geocoder falla, conservamos
                        // las coordenadas GPS.
                    }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {

        EncabezadoPaso(
            titulo = "Locación del reporte",
            subtitulo =
                "Indícanos el lugar exacto del reporte. Puedes buscar la dirección o usar tu ubicación actual.",
            paso = 1,
            alRegresar = alRegresar
        )

        Espacio(28.dp)

        CampoBusqueda(
            valor = estado.direccion,
            alCambiar = {
                vm.actualizarDireccion(it)
            },
            placeholder = "Av. Lago de Guadalupe"
        )

        Espacio(20.dp)

        // MAPA REAL DE MAPBOX
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(11.dp))
        ) {

            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState
            ) {

                // Muestra un pin cuando ya tenemos GPS.
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

            // Botón para usar ubicación actual.
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
        }

        Espacio(20.dp)

        BotonPrincipal(
            texto = "Confirmar",
            alPresionar = {
                vm.siguientePaso()
            }
        )

        Espacio(24.dp)
    }
}