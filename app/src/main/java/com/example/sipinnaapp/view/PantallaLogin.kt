package com.example.sipinnaapp.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipinnaapp.R
import com.example.sipinnaapp.viewmodel.LoginVM

@Composable
fun PantallaLogin(
    vm: LoginVM,
    alIrARegistro: () -> Unit,     // función para navegar a registro
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Espacio(48.dp)

        // Logo SIPINNA
        Image(
            painter = painterResource(id = R.drawable.sipinna_logo),
            contentDescription = "Logo SIPINNA",
            modifier = Modifier.size(120.dp)
        )

        Espacio(16.dp)
        Subtitulo("Inicia sesión")
        Espacio(40.dp)

        CampoTexto(
            etiqueta = "Email",
            valor = estado.email,
            alCambiar = { vm.actualizarEmail(it) },
            teclado = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth()
        )
        Espacio(12.dp)

        CampoPassword(
            valor = estado.password,
            alCambiar = { vm.actualizarPassword(it) },
            modifier = Modifier.fillMaxWidth()
        )
        Espacio(32.dp)

        Button(
            onClick = { vm.login() },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Iniciar sesión", fontSize = 16.sp)
        }

        Espacio(16.dp)

        // Link para ir a registrarse
        TextButton(onClick = alIrARegistro) {
            Text("¿No tienes cuenta? Regístrate")
        }
    }
}
