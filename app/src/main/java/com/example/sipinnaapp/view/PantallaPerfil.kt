package com.example.sipinnaapp.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipinnaapp.ui.theme.*

@Composable
fun PantallaPerfil(
    nombre: String,
    alRegresar: () -> Unit,
    alCerrarSesion: () -> Unit,
    // null = no se muestra (usuario anónimo). Recibe qué hacer si falla.
    alEliminarCuenta: ((alFallar: (String) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var dialogoEliminar by remember { mutableStateOf(false) }
    var confirmacion by remember { mutableStateOf("") }
    var eliminando by remember { mutableStateOf(false) }
    var errorEliminar by remember { mutableStateOf("") }
    val confirmado = confirmacion.trim() == "ELIMINAR"

    fun cerrarDialogo() {
        dialogoEliminar = false
        confirmacion = ""
        errorEliminar = ""
    }

    if (dialogoEliminar && alEliminarCuenta != null) {
        AlertDialog(
            onDismissRequest = { if (!eliminando) cerrarDialogo() },
            title = { Text("Eliminar cuenta") },
            text = {
                Column {
                    Text(
                        "Se borrarán tu cuenta y tus datos personales. Esta acción no se puede deshacer.\n\n" +
                                "Los reportes que ya enviaste se conservan para su seguimiento, pero ya no podrás consultarlos.",
                        fontSize = 14.sp
                    )
                    Espacio(12.dp)
                    Text("Escribe ELIMINAR para confirmar:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Espacio(6.dp)
                    OutlinedTextField(
                        value = confirmacion,
                        onValueChange = { confirmacion = it },
                        singleLine = true,
                        enabled = !eliminando
                    )
                    if (errorEliminar.isNotEmpty()) {
                        Espacio(6.dp)
                        Text(errorEliminar, fontSize = 12.sp, color = RojoNoApto)
                    }
                }
            },
            confirmButton = {
                if (eliminando) {
                    CircularProgressIndicator(color = RojoNoApto, modifier = Modifier.size(24.dp))
                } else {
                    TextButton(
                        enabled = confirmado,
                        onClick = {
                            eliminando = true
                            errorEliminar = ""
                            alEliminarCuenta { mensaje ->
                                eliminando = false
                                errorEliminar = mensaje
                            }
                        }
                    ) { Text("Eliminar", color = if (confirmado) RojoNoApto else GrisTexto) }
                }
            },
            dismissButton = {
                if (!eliminando) TextButton(onClick = { cerrarDialogo() }) { Text("Cancelar") }
            }
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(FondoApp)
            .padding(horizontal = 18.dp)
    ) {
        // Flecha de regreso
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = alRegresar, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar",
                    tint = Color.Black
                )
            }
        }

        Espacio(8.dp)

        // Foto de perfil (círculo grande con borde)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(175.dp)
                .clip(CircleShape)
                .background(SuperficieSecundaria)
                .border(1.dp, Color.Black, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = GrisIcono,
                modifier = Modifier.size(90.dp)
            )
        }

        Espacio(28.dp)

        // Nombre del usuario
        Text(
            text = nombre.ifBlank { "Ciudadano" },
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        Espacio(6.dp)

        Text(
            text = "Este nombre se vinculará con tus reportes.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = GrisTextoClaro,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(188.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        if (alEliminarCuenta != null) {
            TextButton(onClick = { dialogoEliminar = true }) {
                Text("Eliminar mi cuenta", color = GrisTexto, fontSize = 13.sp)
            }
        }

        TextButton(onClick = alCerrarSesion) {
            Text("Cerrar sesión", color = RojoNoApto, fontSize = 15.sp)
        }

        Espacio(24.dp)
    }
}
