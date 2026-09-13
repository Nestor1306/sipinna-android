package com.example.sipinnaapp.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipinnaapp.ui.theme.GrisBarra
import com.example.sipinnaapp.ui.theme.GrisTexto
import com.example.sipinnaapp.ui.theme.Teal
import com.example.sipinnaapp.viewmodel.ReporteVM

@Composable
fun PantallaDescripcion(
    vm: ReporteVM,
    alRegresar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by vm.estado.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        EncabezadoPaso(
            titulo = "Descripción",
            subtitulo = "Una buena descripción del reporte nos ayudará a tomar acción sobre él con una mayor rapidez y calidad.",
            paso = 4,
            alRegresar = alRegresar
        )

        Espacio(28.dp)

        Text(
            text = "¿Qué pasó?",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )

        Espacio(8.dp)

        // Caja grande de texto (ocupa el espacio disponible)
        OutlinedTextField(
            value = estado.descripcion,
            onValueChange = { vm.actualizarDescripcion(it) },
            placeholder = { Text("Vi a unos niños vendiendo dulces y cigarros.", color = GrisTexto) },
            shape = RoundedCornerShape(6.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal,
                unfocusedBorderColor = GrisBarra
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Espacio(20.dp)

        BotonPrincipal(texto = "Siguiente", alPresionar = { vm.siguientePaso() })

        Espacio(24.dp)
    }
}
