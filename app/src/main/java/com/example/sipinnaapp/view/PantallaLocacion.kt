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
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.sipinnaapp.ui.theme.*
import com.example.sipinnaapp.viewmodel.ReporteVM
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

@Composable
fun PantallaLocacion(
    vm: ReporteVM,
    alRegresar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by vm.estado.collectAsState()
    val contexto = LocalContext.current

    // Obtiene la ubicación actual con GPS y la convierte en dirección
    @SuppressLint("MissingPermission")
    fun obtenerUbicacion() {
        vm.actualizarBuscandoUbicacion(true)
        val cliente = LocationServices.getFusedLocationProviderClient(contexto)
        cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { ubicacion ->
                if (ubicacion != null) {
                    vm.actualizarCoordenadas(ubicacion.latitude, ubicacion.longitude)

                    // Geocoder convierte lat/lng en texto ("Av. Lago de Guadalupe...")
                    try {
                        val direcciones = Geocoder(contexto, Locale.getDefault())
                            .getFromLocation(ubicacion.latitude, ubicacion.longitude, 1)
                        val primera = direcciones?.firstOrNull()?.getAddressLine(0)
                        if (!primera.isNullOrEmpty()) vm.actualizarDireccion(primera)
                    } catch (_: Exception) { /* sin internet no hay geocoder, no pasa nada */ }
                } else {
                    vm.actualizarBuscandoUbicacion(false)
                }
            }
            .addOnFailureListener { vm.actualizarBuscandoUbicacion(false) }
    }

    // Pide permiso de ubicación; si lo dan, busca la ubicación
    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> if (concedido) obtenerUbicacion() }

    fun alPresionarMiUbicacion() {
        val yaTienePermiso = ContextCompat.checkSelfPermission(
            contexto, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (yaTienePermiso) obtenerUbicacion()
        else pedirPermiso.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        EncabezadoPaso(
            titulo = "Locación del reporte",
            subtitulo = "Indícanos el lugar exacto del reporte. Puedes buscar la dirección o usar tu ubicación actual.",
            paso = 1,
            alRegresar = alRegresar
        )

        Espacio(28.dp)

        CampoBusqueda(
            valor = estado.direccion,
            alCambiar = { vm.actualizarDireccion(it) },
            placeholder = "Av. Lago de Guadalupe"
        )

        Espacio(20.dp)

        // Área del mapa. Por ahora es un recuadro; cuando tengas la API key
        // de Google Maps aquí va el GoogleMap composable.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(11.dp))
                .background(SuperficieSecundaria)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier.size(46.dp)
                )
                if (estado.latitud != 0.0) {
                    Espacio(8.dp)
                    Text(
                        text = "%.5f, %.5f".format(estado.latitud, estado.longitud),
                        fontSize = 12.sp,
                        color = GrisTexto
                    )
                }
            }

            // Botón "usar mi ubicación" (esquina inferior derecha, como en Figma)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(45.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFFFFFD))
            ) {
                if (estado.buscandoUbicacion) {
                    CircularProgressIndicator(
                        color = Teal,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    IconButton(onClick = { alPresionarMiUbicacion() }) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Usar mi ubicación",
                            tint = Teal
                        )
                    }
                }
            }
        }

        Espacio(20.dp)

        BotonPrincipal(texto = "Confirmar", alPresionar = { vm.siguientePaso() })

        Espacio(24.dp)
    }
}
