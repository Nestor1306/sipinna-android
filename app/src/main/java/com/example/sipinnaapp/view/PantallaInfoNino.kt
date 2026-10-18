package com.example.sipinnaapp.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipinnaapp.ui.theme.GrisTexto
import com.example.sipinnaapp.ui.theme.TextoPrincipal
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
            titulo = "Información del Niño/Niña/Adolescente",
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

            // Pregunta 1: peligro inmediato (va primero: si es una emergencia, el 911 aparece de inmediato)
            EtiquetaPregunta("Pregunta 1")
            Espacio(4.dp)
            TituloPregunta("¿Hay peligro inmediato para el niño, niña o adolescente?")
            Espacio(10.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipOpcion(
                    texto = "Sí",
                    seleccionado = estado.peligroInmediato == true,
                    alPresionar = { vm.seleccionarPeligro(true) }
                )
                ChipOpcion(
                    texto = "No",
                    seleccionado = estado.peligroInmediato == false,
                    alPresionar = { vm.seleccionarPeligro(false) }
                )
            }
            if (estado.peligroInmediato == true) {
                Espacio(10.dp)
                AvisoEmergencia()
            }

            Espacio(28.dp)

            // Pregunta 2: cuántos niños (una sola opción)
            EtiquetaPregunta("Pregunta 2")
            Espacio(4.dp)
            TituloPregunta("¿Cuántos niños, niñas o adolescentes viste?")
            Espacio(10.dp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReporteVM.OPCIONES_CANTIDAD.forEach { opcion ->
                    ChipOpcion(
                        texto = opcion,
                        seleccionado = estado.cantidadNinos == opcion,
                        alPresionar = { vm.seleccionarCantidad(opcion) }
                    )
                }
            }

            Espacio(28.dp)

            // Pregunta 3: edad (una sola opción)
            EtiquetaPregunta("Pregunta 3")
            Espacio(4.dp)
            TituloPregunta("¿Cuál es su edad aproximada?")
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

            // Pregunta 4: tipo de trabajo (varias opciones)
            EtiquetaPregunta("Pregunta 4")
            Espacio(4.dp)
            TituloPregunta("¿Qué tipo de trabajo está realizando?")
            Text(
                text = "Puedes seleccionar más de una opción (\"No sé\" va sola)",
                fontSize = 12.sp,
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

            // Pregunta 5: condición (una sola opción)
            EtiquetaPregunta("Pregunta 5")
            Espacio(4.dp)
            TituloPregunta("¿En qué condición se encuentra?")
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
        BotonPrincipal(
            texto = if (estado.editandoDesdeConfirmar) "Guardar cambios" else "Siguiente",
            alPresionar = { vm.siguientePaso() },
            habilitado = ReporteVM.errorDelPaso(estado).isEmpty()
        )
        Espacio(24.dp)
    }
}

@Composable
fun TituloPregunta(texto: String) {
    Text(
        text = texto,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextoPrincipal
    )
}
// Opciones de la pregunta de riesgo: valor que espera la API → texto en pantalla (igual que la web)
val OPCIONES_RIESGO = listOf("si" to "Sí", "no" to "No", "no_se" to "No sé")

// Pregunta "¿Observaste alguna situación de riesgo?". Es distinta de "peligro inmediato".
// seleccion = "si", "no", "no_se" o "" si todavía no contesta.
@Composable
fun PreguntaRiesgo(numero: Int, seleccion: String, alElegir: (String) -> Unit) {
    EtiquetaPregunta("Pregunta $numero")
    Espacio(4.dp)
    TituloPregunta("¿Observaste alguna situación de riesgo?")
    Espacio(10.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OPCIONES_RIESGO.forEach { (valor, texto) ->
            ChipOpcion(
                texto = texto,
                seleccionado = seleccion == valor,
                alPresionar = { alElegir(valor) }
            )
        }
    }
}
