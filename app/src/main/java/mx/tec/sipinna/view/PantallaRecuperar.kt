package mx.tec.sipinna.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.tec.sipinna.ui.theme.*
import mx.tec.sipinna.viewmodel.RecuperarVM

@Composable
fun PantallaRecuperar(
    vm: RecuperarVM,
    alRegresar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by vm.estado.collectAsState()

    if (estado.error.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { vm.cerrarError() },
            title = { Text("Aviso") },
            text = { Text(estado.error) },
            confirmButton = { TextButton(onClick = { vm.cerrarError() }) { Text("Aceptar") } }
        )
    }

    if (estado.listo) {
        AlertDialog(
            onDismissRequest = alRegresar,
            title = { Text("Contraseña actualizada") },
            text = { Text("Ya puedes iniciar sesión con tu contraseña nueva.") },
            confirmButton = { TextButton(onClick = alRegresar) { Text("Ir a iniciar sesión") } }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        IconButton(onClick = alRegresar, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = TextoPrincipal)
        }

        Text("Recuperar contraseña", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
        Espacio(8.dp)

        if (!estado.codigoEnviado) {
            // --- Paso 1: correo ---
            Text(
                "Escribe el correo o el teléfono de tu cuenta y te enviaremos un código de ${RecuperarVM.DIGITOS_CODIGO} dígitos.",
                fontSize = 14.sp, color = GrisTexto
            )
            Espacio(24.dp)
            CampoConIcono(
                etiqueta = "Correo o teléfono",
                valor = estado.correo,
                alCambiar = { vm.actualizarCorreo(it) },
                icono = Icons.Default.Email,
                ayuda = "tucorreo@ejemplo.com o 5512345678",
                teclado = KeyboardType.Email
            )

            Espacio(28.dp)
            if (estado.cargando) {
                CircularProgressIndicator(color = Teal)
            } else {
                BotonPrincipal(texto = "Enviar código", alPresionar = { vm.pedirCodigo() })
            }
        } else {
            // --- Paso 2: código + contraseña nueva ---
            Text(estado.aviso, fontSize = 14.sp, color = GrisTexto)
            Espacio(4.dp)
            Text("Revisa tu correo (también spam) o tus mensajes SMS.", fontSize = 12.sp, color = GrisTexto)
            Espacio(24.dp)

            CampoConIcono(
                etiqueta = "Código",
                valor = estado.codigo,
                alCambiar = { vm.actualizarCodigo(it) },
                icono = Icons.Default.Lock,
                ayuda = "123456",
                teclado = KeyboardType.Number
            )
            Espacio(14.dp)

            CampoPasswordConOjo(
                etiqueta = "Contraseña nueva",
                valor = estado.nueva,
                alCambiar = { vm.actualizarNueva(it) },
                ayuda = "Mínimo 10 caracteres"
            )
            Espacio(8.dp)
            RequisitosContrasena(estado.nueva)
            Espacio(14.dp)

            CampoPasswordConOjo(
                etiqueta = "Confirmar contraseña nueva",
                valor = estado.confirmar,
                alCambiar = { vm.actualizarConfirmar(it) },
                ayuda = "Repite tu contraseña"
            )
            if (estado.confirmar.isNotEmpty() && estado.confirmar != estado.nueva) {
                Espacio(6.dp)
                Text("Las contraseñas no coinciden", fontSize = 12.sp, color = RojoNoApto)
            }

            Espacio(28.dp)
            if (estado.cargando) {
                CircularProgressIndicator(color = Teal)
            } else {
                BotonPrincipal(texto = "Cambiar contraseña", alPresionar = { vm.cambiarContrasena() })
                Espacio(8.dp)
                Row {
                    TextButton(onClick = { vm.pedirCodigo() }) { Text("Reenviar código", color = Teal) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { vm.cambiarCorreo() }) { Text("Usar otro dato", color = GrisTexto) }
                }
            }
        }
        Espacio(24.dp)
    }
}

