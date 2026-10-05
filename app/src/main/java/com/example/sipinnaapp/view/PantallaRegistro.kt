package com.example.sipinnaapp.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipinnaapp.R
import com.example.sipinnaapp.ui.theme.*
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

    // Fondo con el degradado de la app: es lo que se ve arriba, detrás del logo
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Teal, TealOscuro)))
    ) {

        // Encabezado: logo, nombre y una frase
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.sipinna_logo),
                contentDescription = "Logo SIPINNA",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(8.dp)
            )
            Espacio(10.dp)
            Text(
                text = "SIPINNA",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Crea tu cuenta para dar seguimiento a tus reportes",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
        }

        // Tarjeta blanca con el formulario. Empieza más abajo para dejar ver el encabezado
        // y tiene las esquinas de arriba redondeadas.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 190.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White)
                .padding(top = 16.dp)   // margen fijo arriba: lo que se desliza no toca el borde redondeado
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Espacio(8.dp)

            // Nombre (ocupa todo el ancho)
            CampoConIcono(
                etiqueta = "Nombre",
                valor = estado.nombre,
                alCambiar = { vm.actualizarNombre(it) },
                icono = Icons.Default.Person,
                ayuda = "Tu nombre"
            )

            Espacio(14.dp)

            // Apellido
            CampoConIcono(
                etiqueta = "Apellido",
                valor = estado.apellido,
                alCambiar = { vm.actualizarApellido(it) },
                icono = Icons.Default.Person,
                ayuda = "Tu apellido"
            )

            Espacio(14.dp)

            // Género: lista desplegable para elegir una opción
            SelectorGenero(
                seleccion = estado.genero,
                alElegir = { vm.actualizarGenero(it) }
            )

            Espacio(14.dp)

            // Edad + Teléfono
            FilaDeCampos {
                CampoConIcono(
                    etiqueta = "Edad",
                    valor = estado.edad,
                    alCambiar = { vm.actualizarEdad(it) },
                    icono = Icons.Default.Cake,
                    ayuda = "18",
                    teclado = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                CampoConIcono(
                    etiqueta = "Teléfono",
                    valor = estado.telefono,
                    alCambiar = { vm.actualizarTelefono(it) },
                    icono = Icons.Default.Phone,
                    ayuda = "55 1234 5678",
                    teclado = KeyboardType.Phone,
                    modifier = Modifier.weight(1.7f)
                )
            }

            Espacio(14.dp)

            CampoConIcono(
                etiqueta = "Correo electrónico",
                valor = estado.email,
                alCambiar = { vm.actualizarEmail(it) },
                icono = Icons.Default.Email,
                ayuda = "tucorreo@ejemplo.com",
                teclado = KeyboardType.Email
            )

            Espacio(6.dp)

            Text(
                text = "Puedes ingresar tu teléfono, tu correo o ambos.",
                fontSize = 12.sp,
                color = GrisTexto
            )

            Espacio(14.dp)

            CampoPasswordConOjo(
                etiqueta = "Contraseña",
                valor = estado.password,
                alCambiar = { vm.actualizarPassword(it) },
                ayuda = "Mínimo 6 caracteres"
            )

            Espacio(14.dp)

            CampoPasswordConOjo(
                etiqueta = "Confirmar contraseña",
                valor = estado.confirmarPassword,
                alCambiar = { vm.actualizarConfirmarPassword(it) },
                ayuda = "Repite tu contraseña"
            )

            Espacio(24.dp)

            if (estado.cargando) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(color = Teal)
                }
            } else {
                BotonPrincipal(texto = "Crear cuenta", alPresionar = { vm.registrar() })
            }

            Espacio(8.dp)

            // Enlace para regresar al login (igual que "¿No tienes cuenta? Regístrate")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = alIrALogin) {
                    Text("¿Ya tienes cuenta? Inicia sesión")
                }
            }

            Espacio(16.dp)
        }
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

// --- Componentes del diseño nuevo de registro ---

val OPCIONES_GENERO = listOf("Masculino", "Femenino")

// Texto pequeño que va arriba de cada campo ("Nombre", "Correo electrónico"...)
@Composable
fun EtiquetaCampo(texto: String) {
    Text(
        text = texto,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Black
    )
}

// Lista desplegable para elegir el género.
// Se ve igual que los demás campos, pero al tocarlo abre un menú con las opciones.
@Composable
fun SelectorGenero(
    seleccion: String,               // la opción elegida ("" si todavía no elige)
    alElegir: (String) -> Unit
) {
    // Recuerda si el menú está abierto o cerrado
    var abierto by remember { mutableStateOf(false) }

    Column {
        EtiquetaCampo("Género")
        Espacio(6.dp)

        Box {
            // El "campo": al tocarlo se abre el menú
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SuperficiePrimaria)
                    .border(1.dp, if (abierto) Teal else GrisBarra, RoundedCornerShape(14.dp))
                    .clickable { abierto = true }
                    .padding(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.Wc, contentDescription = null, tint = GrisIcono)
                Spacer(Modifier.width(16.dp))
                Text(
                    text = seleccion.ifEmpty { "Selecciona tu género" },
                    fontSize = if (seleccion.isEmpty()) 14.sp else 16.sp,
                    color = if (seleccion.isEmpty()) GrisTextoClaro else Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Abrir opciones", tint = GrisIcono)
            }

            // El menú con las opciones. Al elegir una, se guarda y se cierra.
            DropdownMenu(
                expanded = abierto,
                onDismissRequest = { abierto = false }
            ) {
                OPCIONES_GENERO.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion) },
                        onClick = {
                            alElegir(opcion)
                            abierto = false
                        }
                    )
                }
            }
        }
    }
}

// Campo de texto con su etiqueta arriba y un ícono a la izquierda
@Composable
fun CampoConIcono(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    icono: ImageVector,
    ayuda: String,                       // texto gris que se ve cuando el campo está vacío
    teclado: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        EtiquetaCampo(etiqueta)
        Espacio(6.dp)
        OutlinedTextField(
            value = valor,
            onValueChange = alCambiar,
            placeholder = { Text(ayuda, fontSize = 14.sp, color = GrisTextoClaro) },
            leadingIcon = { Icon(icono, contentDescription = null, tint = GrisIcono) },
            keyboardOptions = KeyboardOptions(keyboardType = teclado),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal,
                unfocusedBorderColor = GrisBarra,
                focusedContainerColor = SuperficiePrimaria,
                unfocusedContainerColor = SuperficiePrimaria
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Campo de contraseña con candado a la izquierda y un "ojo" a la derecha
// para mostrar u ocultar lo que se escribe
@Composable
fun CampoPasswordConOjo(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    ayuda: String,
    modifier: Modifier = Modifier
) {
    // Recuerda si la contraseña se está mostrando o no
    var visible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        EtiquetaCampo(etiqueta)
        Espacio(6.dp)
        OutlinedTextField(
            value = valor,
            onValueChange = alCambiar,
            placeholder = { Text(ayuda, fontSize = 14.sp, color = GrisTextoClaro) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GrisIcono) },
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = GrisIcono
                    )
                }
            },
            // Si no está visible, los caracteres se cambian por puntos
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal,
                unfocusedBorderColor = GrisBarra,
                focusedContainerColor = SuperficiePrimaria,
                unfocusedContainerColor = SuperficiePrimaria
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
