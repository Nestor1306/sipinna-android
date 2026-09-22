package com.example.sipinnaapp.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                TextButton(onClick = {
                    vm.reiniciar()
                    alIrALogin()   // después de registrarse, pasa a iniciar sesión
                }) { Text("Continuar") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Espacio(16.dp)

        Text("Regístrate", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Espacio(4.dp)
        Subtitulo("Crea tu cuenta para comenzar")

        Espacio(28.dp)

        // Nombre + Apellido
        FilaDeCampos {
            CampoTexto(
                etiqueta = "Nombre",
                valor = estado.nombre,
                alCambiar = { vm.actualizarNombre(it) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            CampoTexto(
                etiqueta = "Apellido",
                valor = estado.apellido,
                alCambiar = { vm.actualizarApellido(it) },
                modifier = Modifier.weight(1f)
            )
        }

        Espacio(12.dp)

        // Género + Edad
        FilaDeCampos {
            CampoTexto(
                etiqueta = "Género",
                valor = estado.genero,
                alCambiar = { vm.actualizarGenero(it) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            CampoTexto(
                etiqueta = "Edad",
                valor = estado.edad,
                alCambiar = { vm.actualizarEdad(it) },
                teclado = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
        }

        Espacio(12.dp)

        // Número ó Correo
        FilaDeCampos {
            CampoTexto(
                etiqueta = "Número",
                valor = estado.telefono,
                alCambiar = { vm.actualizarTelefono(it) },
                teclado = KeyboardType.Phone,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "ó",
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            CampoTexto(
                etiqueta = "Correo electrónico",
                valor = estado.email,
                alCambiar = { vm.actualizarEmail(it) },
                teclado = KeyboardType.Email,
                modifier = Modifier.weight(1f)
            )
        }

        Espacio(6.dp)

        Text(
            text = "Puedes ingresar tu número, tu correo o ambos.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Espacio(12.dp)

        // Contraseña + Confirmar
        FilaDeCampos {
            CampoPassword(
                etiqueta = "Contraseña",
                valor = estado.password,
                alCambiar = { vm.actualizarPassword(it) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            CampoPassword(
                etiqueta = "Confirmar",
                valor = estado.confirmarPassword,
                alCambiar = { vm.actualizarConfirmarPassword(it) },
                modifier = Modifier.weight(1f)
            )
        }

        Espacio(28.dp)

        if (estado.cargando) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator()
            }
        } else {
            BotonRegistrar(alPresionar = { vm.registrar() })
        }

        Espacio(12.dp)

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = alIrALogin) {
                Text("¿Ya tienes cuenta? Inicia sesión")
            }
        }

        Espacio(24.dp)
    }
}

// --- Componentes reutilizables ---

// Agrupa dos campos lado a lado, alineados por arriba
@Composable
fun FilaDeCampos(contenido: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(),
        content = contenido
    )
}

@Composable
fun Titulo(texto: String) {
    Text(text = texto, fontSize = 32.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun Subtitulo(texto: String) {
    Text(text = texto, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun CampoTexto(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    teclado: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
fun CampoPassword(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String = "Contraseña",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
fun BotonRegistrar(alPresionar: () -> Unit) {
    Button(
        onClick = alPresionar,
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text(text = "Crear cuenta", fontSize = 16.sp)
    }
}

@Composable
fun Espacio(altura: Dp) {
    Spacer(modifier = Modifier.height(altura))
}
