/*
 * SIPINNA — PantallaFotos.kt
 * Capa: View (pantallas y componentes de Jetpack Compose)
 *
 * Paso 2 del reporte: fotos del lugar desde la cámara o la galería.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 *   - Néstor Daniel Huidobro Aguilar
 * Creado: 2026-09-13
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import mx.tec.sipinna.ui.theme.*
import mx.tec.sipinna.viewmodel.ReporteVM
import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * Paso 2 del reporte: hasta [ReporteVM.MAX_FOTOS] fotos del lugar.
 * Al tocar "+" pregunta si se usa la cámara o la galería.
 * @param vm ViewModel del reporte (guarda las fotos elegidas).
 * @param alRegresar regresa al paso anterior.
 */
@Composable
fun PantallaFotos(
    vm: ReporteVM,
    alRegresar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by vm.estado.collectAsState()

    val contexto = LocalContext.current

    // Diálogo "¿Cámara o galería?"
    var preguntarOrigen by remember { mutableStateOf(false) }

    // Abre la galería del teléfono y permite elegir varias fotos
    val elegirFotos = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(ReporteVM.MAX_FOTOS)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) vm.agregarFotos(uris.map { it.toString() })
    }

    // Abre la cámara. La foto se guarda en el archivo que le pasamos (fotoPendiente).
    // Se guarda con rememberSaveable por si Android cierra la app mientras la cámara está abierta.
    var fotoPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    val tomarFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { tomada: Boolean ->
        val uri = fotoPendiente
        if (tomada && uri != null) {
            vm.agregarFotos(listOf(uri))
        } else if (uri != null) {
            borrarFotoDeCamara(contexto, Uri.parse(uri))   // se canceló: el archivo quedó vacío
        }
        fotoPendiente = null
    }

    if (preguntarOrigen) {
        AlertDialog(
            onDismissRequest = { preguntarOrigen = false },
            title = { Text("Agregar foto") },
            text = {
                Column {
                    OpcionOrigenFoto(Icons.Default.PhotoCamera, "Tomar foto") {
                        preguntarOrigen = false
                        val uri = nuevoArchivoParaFoto(contexto)
                        fotoPendiente = uri.toString()
                        try {
                            tomarFoto.launch(uri)
                        } catch (e: ActivityNotFoundException) {
                            fotoPendiente = null
                            Toast.makeText(contexto, "No se encontró una app de cámara", Toast.LENGTH_SHORT).show()
                        }
                    }
                    OpcionOrigenFoto(Icons.Default.PhotoLibrary, "Elegir de la galería") {
                        preguntarOrigen = false
                        elegirFotos.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { preguntarOrigen = false }) { Text("Cancelar", color = Teal) }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        EncabezadoPaso(
            titulo = "Fotos del lugar",
            subtitulo = "Una foto del lugar del reporte nos ayudará a determinar la calidez de tu reporte y aportará a futuras contribuciones.",
            paso = 2,
            alRegresar = alRegresar
        )

        Espacio(20.dp)

        // Una fila de 2 cuadros: las fotos elegidas + el cuadro para agregar (máximo 2 fotos)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            estado.fotos.forEach { uri ->
                TarjetaFoto(uri = uri, alQuitar = { vm.quitarFoto(uri) }, modifier = Modifier.weight(1f))
            }
            if (estado.fotos.size < ReporteVM.MAX_FOTOS) {
                CuadroAgregarFoto(
                    alPresionar = { preguntarOrigen = true },
                    modifier = Modifier.weight(1f)
                )
            }
            // Si no hay fotos, un espacio vacío para que el cuadro "+" no ocupe todo el ancho
            if (estado.fotos.isEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Espacio(8.dp)
        Text(
            text = "${estado.fotos.size} de ${ReporteVM.MAX_FOTOS} fotos" +
                if (ReporteVM.FOTO_OBLIGATORIA) " (mínimo 1)" else " (opcional)",
            fontSize = 12.sp,
            color = GrisTexto
        )

        Spacer(modifier = Modifier.weight(1f))

        BotonPrincipal(texto = if (estado.editandoDesdeConfirmar) "Guardar cambios" else "Siguiente", alPresionar = { vm.siguientePaso() })

        Espacio(24.dp)
    }
}

/** Foto elegida con botón "x" para quitarla */
@Composable
fun TarjetaFoto(uri: String, alQuitar: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "Foto del lugar",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = alQuitar)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Quitar", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

/** Cuadro gris con ícono para agregar una foto */
@Composable
fun CuadroAgregarFoto(alPresionar: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(SuperficieSecundaria)
            .clickable(onClick = alPresionar)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Agregar foto",
            tint = TextoPrincipal,
            modifier = Modifier.size(48.dp)
        )
    }
}


/** Una opción del diálogo "Agregar foto" (ícono + texto, toda la fila se puede tocar) */
@Composable
private fun OpcionOrigenFoto(icono: ImageVector, texto: String, alPresionar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = alPresionar)
            .padding(vertical = 14.dp, horizontal = 4.dp)
    ) {
        Icon(icono, contentDescription = null, tint = Teal, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(16.dp))
        Text(texto, fontSize = 16.sp, color = TextoPrincipal)
    }
}

/** Crea un archivo vacío en la caché de la app para que la cámara guarde ahí la foto */
private fun nuevoArchivoParaFoto(contexto: Context): Uri {
    val carpeta = File(contexto.cacheDir, "fotos").apply { mkdirs() }
    val archivo = File(carpeta, "foto_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(contexto, "${contexto.packageName}.fotos", archivo)
}

/** Borra el archivo de una foto de cámara que se canceló */
private fun borrarFotoDeCamara(contexto: Context, uri: Uri) {
    runCatching { contexto.contentResolver.delete(uri, null, null) }
}
