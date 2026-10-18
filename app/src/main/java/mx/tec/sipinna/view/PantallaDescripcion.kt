package mx.tec.sipinna.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.tec.sipinna.ui.theme.GrisBarra
import mx.tec.sipinna.ui.theme.GrisTexto
import mx.tec.sipinna.ui.theme.TextoPrincipal
import mx.tec.sipinna.ui.theme.Teal
import mx.tec.sipinna.viewmodel.ReporteVM
import androidx.compose.ui.Alignment
import mx.tec.sipinna.ui.theme.VerdeConfirmado

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
            color = TextoPrincipal
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

        // Contador de palabras: gris mientras falta, verde cuando ya alcanza el mínimo
        val palabras = ReporteVM.contarPalabras(estado.descripcion)
        val suficiente = palabras >= ReporteVM.MIN_PALABRAS
        Espacio(6.dp)
        Text(
            text = if (suficiente) "$palabras palabras ✓"
            else "$palabras de ${ReporteVM.MIN_PALABRAS} palabras mínimo",
            fontSize = 12.sp,
            color = if (suficiente) VerdeConfirmado else GrisTexto,
            modifier = Modifier.align(Alignment.End)
        )

        Espacio(14.dp)

        BotonPrincipal(
            texto = if (estado.editandoDesdeConfirmar) "Guardar cambios" else "Siguiente",
            alPresionar = { vm.siguientePaso() },
            habilitado = suficiente
        )

        Espacio(24.dp)
    }
}
