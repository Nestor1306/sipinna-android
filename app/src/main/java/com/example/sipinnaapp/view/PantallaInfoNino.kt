package com.example.sipinnaapp.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipinnaapp.ui.theme.GrisTexto
import com.example.sipinnaapp.viewmodel.ReporteVM

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaInfoNino(
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
            titulo = "Información del niño",
            subtitulo = "Estos datos nos ayudan a entender mejor la situación.",
            paso = 3,
            alRegresar = alRegresar
        )

        // Contenido con scroll por si no cabe en pantallas chicas
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Espacio(24.dp)

            // Pregunta 1: edad (una sola opción)
            EtiquetaPregunta("Pregunta 1")
            Espacio(4.dp)
            TituloPregunta("¿Cuál es la edad aproximada del niño?")
            Espacio(10.dp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReporteVM.OPCIONES_EDAD.forEach { opcion ->
                    ChipOpcion(
                        texto = opcion,
                        seleccionado = estado.edadNino == opcion,
                        alPresionar = { vm.seleccionarEdad(opcion) }
                    )
                }
            }

            Espacio(28.dp)

            // Pregunta 2: tipo de trabajo (varias opciones)
            EtiquetaPregunta("Pregunta 2")
            Espacio(4.dp)
            TituloPregunta("¿Qué tipo de trabajo está realizando?")
            Text(
                text = "Puedes seleccionar más de una opción",
                fontSize = 10.sp,
                color = GrisTexto
            )
            Espacio(10.dp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReporteVM.OPCIONES_TRABAJO.forEach { opcion ->
                    ChipOpcion(
                        texto = opcion,
                        seleccionado = opcion in estado.tiposTrabajo,
                        alPresionar = { vm.alternarTipoTrabajo(opcion) }
                    )
                }
            }

            Espacio(28.dp)

            // Pregunta 3: condición (una sola opción)
            EtiquetaPregunta("Pregunta 3")
            Espacio(4.dp)
            TituloPregunta("¿En qué condición se encuentra el niño?")
            Espacio(10.dp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReporteVM.OPCIONES_CONDICION.forEach { opcion ->
                    ChipOpcion(
                        texto = opcion,
                        seleccionado = estado.condicion == opcion,
                        alPresionar = { vm.seleccionarCondicion(opcion) }
                    )
                }
            }

            Espacio(24.dp)
        }

        Espacio(12.dp)
        BotonPrincipal(texto = "Siguiente", alPresionar = { vm.siguientePaso() })
        Espacio(24.dp)
    }
}

@Composable
fun TituloPregunta(texto: String) {
    Text(
        text = texto,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.Black
    )
}
