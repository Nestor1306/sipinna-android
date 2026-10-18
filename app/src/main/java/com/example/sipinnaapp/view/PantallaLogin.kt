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
import androidx.compose.ui.text.style.TextDecoration
import com.example.sipinnaapp.ui.theme.GrisTexto
import com.example.sipinnaapp.ui.theme.Teal


@Composable
fun PantallaLogin(
    vm: LoginVM,
    alIrARegistro: () -> Unit, // función para navegar a registro
    alEntrarAnonimo:() -> Unit,
    alOlvideContrasena: () -> Unit,
    alVerCreditos: () -> Unit,
    modifier: Modifier = Modifier,
    // null = todavía no se muestra el enlace (falta conectar la consulta pública)
    alConsultarReporte: (() -> Unit)? = null
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
            etiqueta = "Correo o teléfono",
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
        // Link para recuperar la contraseña (a la derecha, debajo del campo)
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = alOlvideContrasena) {
                Text("¿Olvidaste tu contraseña?", fontSize = 13.sp)
            }
        }
        Espacio(16.dp)

        if (estado.cargando) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { vm.login() },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Iniciar sesión", fontSize = 16.sp)
            }
        }

        Espacio(16.dp)

        // Link para ir a registrarse
        TextButton(onClick = alIrARegistro) {
            Text("¿No tienes cuenta? Regístrate")
        }

        //Para entrar anonimo
        TextButton(onClick = alEntrarAnonimo) {
            Text(
                text = "Entrar sin registrarme",
                color = GrisTexto,
                textDecoration = TextDecoration.Underline
            )
        }

        // Ver cómo va un reporte enviado sin cuenta (con folio y clave de consulta)
        if (alConsultarReporte != null) {
            TextButton(onClick = alConsultarReporte) {
                Text("¿Hiciste un reporte sin cuenta? Consúltalo aquí", fontSize = 13.sp, color = Teal)
            }
        }

        Spacer(Modifier.weight(1f))   // empuja el enlace hasta abajo

        TextButton(onClick = alVerCreditos) {
            Text("Acerca de la app", fontSize = 13.sp, color = GrisTexto)
        }
    }
}
