package mx.tec.sipinna.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.tec.sipinna.ui.theme.*
import mx.tec.sipinna.viewmodel.ReporteVM
import androidx.compose.ui.draw.alpha
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Call
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow

// --- Componentes reutilizables del flujo de reporte (según Figma) ---

// Flecha de regreso + título + subtítulo + barra de progreso
@Composable
fun EncabezadoPaso(
    titulo: String,
    subtitulo: String,
    paso: Int,
    alRegresar: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = alRegresar, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Regresar",
                tint = TextoPrincipal
            )
        }

        Text(
            text = titulo,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = TextoPrincipal
        )
        Espacio(4.dp)
        Text(
            text = subtitulo,
            fontSize = 12.sp,
            color = GrisTexto,
            lineHeight = 14.sp
        )
        Espacio(16.dp)
        BarraProgreso(paso = paso, total = ReporteVM.TOTAL_PASOS)
    }
}

// Línea de progreso con degradado teal → azul
@Composable
fun BarraProgreso(paso: Int, total: Int) {
    val fraccion = paso.toFloat() / total.toFloat()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(41.dp))
            .background(GrisBarra)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraccion)
                .fillMaxHeight()
                .clip(RoundedCornerShape(41.dp))
                .background(
                    Brush.horizontalGradient(listOf(ProgresoInicio, ProgresoFin))
                )
        )
    }
}

// Botón grande con degradado teal (Confirmar / Siguiente / Enviar)
@Composable
fun BotonPrincipal(
    texto: String,
    alPresionar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true
) {
    val forma = RoundedCornerShape(60.dp)

    Button(
        onClick = alPresionar,
        enabled = habilitado,
        shape = forma,
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .alpha(if (habilitado) 1f else 0.4f)   // deshabilitado se ve deslavado
            .background(
                brush = Brush.horizontalGradient(
                    listOf(Teal, TealOscuro),
                    startX = 0f,
                    endX = 2000f   // el degradado termina lejos, como en Figma (217%)
                ),
                shape = forma
            )
    ) {
        Text(
            text = texto,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

// Chip de opción (pantalla "Información del niño")
@Composable
fun ChipOpcion(
    texto: String,
    seleccionado: Boolean,
    alPresionar: () -> Unit
) {
    val forma = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .clip(forma)
            .background(if (seleccionado) Teal else Color.White)
            .border(1.dp, if (seleccionado) Teal else GrisBarra, forma)
            .clickable(onClick = alPresionar)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (seleccionado) Color.White else TextoPrincipal
        )
    }
}

// Etiqueta pequeña tipo "PREGUNTA 1"
@Composable
fun EtiquetaPregunta(texto: String) {
    Text(
        text = texto.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Teal,
        letterSpacing = 1.sp
    )
}

// Barra de búsqueda gris redondeada (Home y Locación)
@Composable
fun CampoBusqueda(
    valor: String,
    alCambiar: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    TextField(
        value = valor,
        onValueChange = alCambiar,
        placeholder = {
            Text(
                placeholder, fontSize = 13.sp, color = GrisIcono,
                maxLines = 1, overflow = TextOverflow.Ellipsis   // con letra grande: "Busca por folio o…"
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = GrisIcono,
                modifier = Modifier.size(19.dp)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(32.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SuperficieSecundaria,
            unfocusedContainerColor = SuperficieSecundaria,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// Botón circular (el "+" y el "≡" del Home)
@Composable
fun BotonCircular(
    fondo: Color,
    alPresionar: () -> Unit,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(fondo)
            .clickable(onClick = alPresionar)
    ) {
        contenido()
    }
}

// Aviso para emergencias: un reporte en la app no es inmediato.
// "Llamar" abre el marcador con el 911 escrito; la persona decide si llama.
@Composable
fun AvisoEmergencia(modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val abrirMarcador = {
        contexto.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:911")))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RojoNoApto.copy(alpha = 0.10f))
            .clickable { abrirMarcador() }
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Text(
            text = "¿Un niño está en peligro inmediato? Llama al 911.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = RojoNoApto,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = abrirMarcador,
            colors = ButtonDefaults.buttonColors(containerColor = RojoNoApto, contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Llamar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}