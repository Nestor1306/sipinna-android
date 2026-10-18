package mx.tec.sipinna.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.tec.sipinna.ui.theme.*

// Datos del perfil tal como se ven en la pantalla.
// El género va con el texto de la lista (OPCIONES_GENERO); el ViewModel lo traduce
// a los valores de la API (hombre, mujer, otro, prefiero-no-decir).
data class DatosPerfil(
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",     // 10 dígitos, sin +52
    val edad: String = "",
    val genero: String = ""
)

// Ver y editar los datos de la cuenta (GET /account y PUT /account).
// La pantalla no hace las peticiones: muestra lo que le pasen y avisa con alGuardar.
@Composable
fun PantallaEditarPerfil(
    datos: DatosPerfil,
    alCambiar: (DatosPerfil) -> Unit,
    alGuardar: () -> Unit,
    alRegresar: () -> Unit,
    modifier: Modifier = Modifier,
    cargando: Boolean = false,              // trayendo los datos del servidor
    guardando: Boolean = false,
    error: String = "",                     // por ejemplo "Ese correo ya está registrado."
    guardado: Boolean = false,              // se guardó bien: muestra el aviso verde
    puedeCambiarCorreo: Boolean = true      // false si la cuenta no tiene contraseña (has_password)
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        IconButton(onClick = alRegresar, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = TextoPrincipal)
        }

        Text("Mis datos", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
        Espacio(8.dp)
        Text(
            text = "Puedes ingresar tu teléfono, tu correo o ambos.",
            fontSize = 14.sp,
            color = GrisTexto
        )

        Espacio(24.dp)

        if (cargando) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(color = Teal)
            }
            return@Column
        }

        CampoConIcono(
            etiqueta = "Nombre completo",
            valor = datos.nombre,
            alCambiar = { alCambiar(datos.copy(nombre = it)) },
            icono = Icons.Default.Person,
            ayuda = "Tu nombre"
        )
        Espacio(16.dp)

        CampoConIcono(
            etiqueta = "Correo electrónico",
            valor = datos.correo,
            alCambiar = { if (puedeCambiarCorreo) alCambiar(datos.copy(correo = it.trim())) },
            icono = Icons.Default.Email,
            ayuda = "tucorreo@ejemplo.com",
            teclado = KeyboardType.Email
        )
        if (!puedeCambiarCorreo) {
            Espacio(4.dp)
            Text(
                text = "Para cambiar tu correo primero crea una contraseña con \"¿Olvidaste tu contraseña?\".",
                fontSize = 12.sp,
                color = GrisTexto
            )
        }
        Espacio(16.dp)

        CampoConIcono(
            etiqueta = "Teléfono",
            valor = datos.telefono,
            alCambiar = { alCambiar(datos.copy(telefono = it.filter { c -> c.isDigit() }.take(10))) },
            icono = Icons.Default.Phone,
            ayuda = "55 1234 5678",
            teclado = KeyboardType.Number
        )
        Espacio(16.dp)

        FilaDeCampos {
            CampoConIcono(
                etiqueta = "Edad",
                valor = datos.edad,
                alCambiar = { alCambiar(datos.copy(edad = it.filter { c -> c.isDigit() }.take(3))) },
                icono = Icons.Default.Cake,
                ayuda = "Edad",
                teclado = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            SelectorGenero(
                seleccion = datos.genero,
                alElegir = { alCambiar(datos.copy(genero = it)) },
                modifier = Modifier.weight(1f)
            )
        }

        if (error.isNotEmpty()) {
            Espacio(12.dp)
            Text(error, fontSize = 13.sp, color = RojoNoApto)
        } else if (guardado) {
            Espacio(12.dp)
            Text("Tus datos se guardaron.", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = VerdeConfirmado)
        }

        Espacio(24.dp)

        if (guardando) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(color = Teal)
            }
        } else {
            BotonPrincipal(
                texto = "Guardar cambios",
                alPresionar = alGuardar,
                habilitado = datos.nombre.isNotBlank() && (datos.correo.isNotBlank() || datos.telefono.isNotBlank())
            )
        }

        Espacio(24.dp)
    }
}

// Vista previa para Android Studio (no se usa en la app)
@Preview(showBackground = true)
@Composable
private fun VistaPreviaEditarPerfil() {
    SipinnaAppTheme {
        PantallaEditarPerfil(
            datos = DatosPerfil(
                nombre = "Ana López",
                correo = "ana@ejemplo.com",
                telefono = "5512345678",
                edad = "30",
                genero = "Femenino"
            ),
            alCambiar = {},
            alGuardar = {},
            alRegresar = {}
        )
    }
}
