package com.example.sipinnaapp.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextAlign
import com.example.sipinnaapp.ui.theme.*
import com.example.sipinnaapp.viewmodel.ReporteVM
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString

@Composable
fun PantallaReporteEnviado(
    vm: ReporteVM,
    alCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by vm.estado.collectAsState()
    var mostrarInfo by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SuperficiePrimaria)
            .padding(horizontal = 18.dp)
    ) {
        // X para cerrar e ícono de info
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = alCerrar, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextoPrincipal, modifier = Modifier.size(28.dp))
            }
            IconButton(onClick = { mostrarInfo = true }) {
                Icon(Icons.Default.Info, contentDescription = "Información", tint = TextoPrincipal, modifier = Modifier.size(28.dp))
            }
        }

        // Qué pasa con el reporte después de enviarlo
        if (mostrarInfo) {
            AlertDialog(
                onDismissRequest = { mostrarInfo = false },
                title = { Text("¿Qué sigue?") },
                text = {
                    Text(
                        text = "El personal de SIPINNA revisará tu reporte y le dará seguimiento. " +
                            "Su estado puede ser: recibido, en revisión, canalizado, en atención o concluido.\n\n" +
                            "Si tienes cuenta, lo verás en tu inicio. Si no, guarda tu folio y tu clave de consulta " +
                            "para revisarlo en www.sipinna.com/consultar-folio.\n\n" +
                            "Si una niña, niño o adolescente está en peligro en este momento, llama al 911.",
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { mostrarInfo = false }) { Text("Entendido", color = Teal) }
                }
            )
        }

        Text(
            text = if (estado.guardadoSinConexion) "Reporte guardado" else "Reporte enviado",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = if (estado.guardadoSinConexion) AmarilloProgreso else VerdeConfirmado
        )

        Espacio(6.dp)

        // Estado real que devolvió el servidor (normalmente "en progreso")
        val info = infoDeEstado(estado.estadoGenerado)
        Text(
            text = "Estado: ${info.titulo}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = info.color
        )

        Espacio(6.dp)

        if (estado.guardadoSinConexion) {
            // No había internet: quedó guardado en el teléfono
            Text(
                text = "No hay conexión a internet. Tu reporte quedó guardado en el teléfono y se enviará automáticamente en cuanto vuelva la conexión, aunque cierres la app.",
                fontSize = 12.sp,
                color = GrisTexto,
                textAlign = TextAlign.Center
            )
        } else {
            if (estado.enviadoAnonimo) {
                Text(
                    text = "Enviado de forma anónima",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal
                )
                Espacio(4.dp)
            }

            // Folio: comprobante de que el reporte llegó (se toca para copiarlo)
            if (estado.folioGenerado.startsWith("RIETI")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Folio: ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
                    FolioCopiable(estado.folioGenerado, tamano = 14.sp)
                }
            } else {
                Text(
                    text = "Folio: ${estado.folioGenerado}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal
                )
            }

            // Clave de consulta: junto con el folio permite ver el estado del reporte sin cuenta
            if (estado.claveConsulta.isNotBlank()) {
                Espacio(6.dp)
                Text("Clave de consulta:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
                TextoCopiable(estado.claveConsulta, avisoCopiado = "Clave copiada")
                Espacio(8.dp)
                Text(
                    text = "Guarda tu folio y tu clave: con ellos puedes ver cómo va tu reporte, aunque no tengas cuenta, en www.sipinna.com/consultar-folio.",
                    fontSize = 12.sp,
                    color = GrisTexto,
                    textAlign = TextAlign.Center
                )
            } else if (estado.enviadoAnonimo) {
                Espacio(8.dp)
                Text(
                    text = "Como no iniciaste sesión, no podrás consultar su estado. Crea una cuenta si quieres darle seguimiento a tus próximos reportes.",
                    fontSize = 12.sp,
                    color = GrisTexto,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Aviso si alguna foto no se pudo subir (el reporte sí quedó guardado)
        if (estado.avisoFotos.isNotEmpty()) {
            Espacio(8.dp)
            Text(
                text = estado.avisoFotos,
                fontSize = 12.sp,
                color = RojoNoApto
            )
        }

        Espacio(32.dp)

        // Tarjeta de agradecimiento
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "¡Gracias por contribuir!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal
                )

                if (estado.fotos.isNotEmpty()) {
                    Espacio(16.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        estado.fotos.forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Foto del lugar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }

                Espacio(16.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .padding(12.dp)
                ) {
                    Text(estado.descripcion, fontSize = 13.sp, color = TextoPrincipal)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        BotonPrincipal(texto = "Volver al inicio", alPresionar = alCerrar)

        Espacio(24.dp)
    }
}

// Texto con un botón para copiarlo (la clave de consulta es larga, puede ocupar dos renglones)
@Composable
private fun TextoCopiable(texto: String, avisoCopiado: String) {
    val portapapeles = LocalClipboardManager.current
    val contexto = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable {
                portapapeles.setText(AnnotatedString(texto))
                Toast.makeText(contexto, avisoCopiado, Toast.LENGTH_SHORT).show()
            }
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = texto,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextoPrincipal,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = Teal, modifier = Modifier.size(16.dp))
    }
}
