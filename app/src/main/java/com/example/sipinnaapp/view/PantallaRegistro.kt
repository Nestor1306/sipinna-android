package com.example.sipinnaapp.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipinnaapp.R
import com.example.sipinnaapp.viewmodel.RegistroVM

@Composable
fun PantallaRegistro(
    vm: RegistroVM,
    alIrALogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val estado by vm.estado.collectAsState()

    if (estado.error.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { vm.cerrarError() },
            title = { Text("Aviso") },
            text = { Text(estado.error) },
            confirmButton = {
                TextButton(onClick = { vm.cerrarError() }) { Text("Aceptar") }
            }
        )
    }

    if (estado.registroExitoso) {
        AlertDialog(
            onDismissRequest = { vm.reiniciar() },
            title = { Text("¡Registro exitoso!") },
            text = { Text("Bienvenido, ${estado.nombre}") },
            confirmButton = {
                TextButton(onClick = { vm.reiniciar() }) { Text("Continuar") }
            }
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Espacio(24.dp)

        // Logo SIPINNA
        Image(
            painter = painterResource(id = R.drawable.sipinna_logo),
            contentDescription = "Logo SIPINNA",
            modifier = Modifier.size(120.dp)
        )

        Espacio(16.dp)
        Subtitulo("Crea tu cuenta")
        Espacio(32.dp)

        CampoTexto(
            etiqueta = "Nombre",
            valor = estado.nombre,
            alCambiar = { vm.actualizarNombre(it) }
        )
        Espacio(12.dp)

        CampoTexto(
            etiqueta = "Email",
            valor = estado.email,
            alCambiar = { vm.actualizarEmail(it) },
            teclado = KeyboardType.Email
        )
        Espacio(12.dp)

        CampoPassword(
            valor = estado.password,
            alCambiar = { vm.actualizarPassword(it) }
        )
        Espacio(32.dp)

        // Muestra spinner mientras espera respuesta del servidor
        if (estado.cargando) {
            CircularProgressIndicator()
        } else {
            BotonRegistrar(alPresionar = { vm.registrar() })
        }
        Espacio(16.dp)

        TextButton(onClick = alIrALogin) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}

// --- Componentes reutilizables ---

@Composable
fun Titulo(texto: String) {
    Text(text = texto, fontSize = 32.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun Subtitulo(texto: String) {
    Text(text = texto, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun CampoTexto(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    teclado: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun CampoPassword(valor: String, alCambiar: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text("Contraseña") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun BotonRegistrar(alPresionar: () -> Unit) {
    Button(
        onClick = alPresionar,
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text(text = "Registrarse", fontSize = 16.sp)
    }
}

@Composable
fun Espacio(altura: Dp) {
    Spacer(modifier = Modifier.height(altura))
}
