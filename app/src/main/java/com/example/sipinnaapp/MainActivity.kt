package com.example.sipinnaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.sipinnaapp.ui.theme.SipinnaAppTheme
import com.example.sipinnaapp.view.PantallaConfirmar
import com.example.sipinnaapp.view.PantallaDescripcion
import com.example.sipinnaapp.view.PantallaFotos
import com.example.sipinnaapp.view.PantallaHome
import com.example.sipinnaapp.view.PantallaInfoNino
import com.example.sipinnaapp.view.PantallaLocacion
import com.example.sipinnaapp.view.PantallaLogin
import com.example.sipinnaapp.view.PantallaPerfil
import com.example.sipinnaapp.view.PantallaRegistro
import com.example.sipinnaapp.view.PantallaReporteEnviado
import com.example.sipinnaapp.viewmodel.LoginVM
import com.example.sipinnaapp.viewmodel.RegistroVM
import com.example.sipinnaapp.viewmodel.ReporteVM
import com.example.sipinnaapp.view.PantallaDetalleReporte

// Cuánto tarda el cambio suave entre pantallas (en milisegundos)
const val DURACION_TRANSICION = 400

class MainActivity : ComponentActivity() {

    private val registroVM: RegistroVM by viewModels()
    private val loginVM: LoginVM by viewModels()
    private val reporteVM: ReporteVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SipinnaAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavegacion(
                        registroVM = registroVM,
                        loginVM = loginVM,
                        reporteVM = reporteVM,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Controla qué pantalla se muestra
// "login", "registro", "home", "perfil" o "reporte"
@Composable
fun AppNavegacion(
    registroVM: RegistroVM,
    loginVM: LoginVM,
    reporteVM: ReporteVM,
    modifier: Modifier = Modifier
) {
    val estadoLogin by loginVM.estado.collectAsState()
    val estadoReporte by reporteVM.estado.collectAsState()

    // Guardamos en qué pantalla estamos.
    // Si ya había una sesión guardada, empezamos directo en el Home.
    var pantallaActual by remember {
        mutableStateOf(
            if (estadoLogin.loginExitoso || estadoLogin.esAnonimo) "home" else "login"
        )
    }

    // Folio del reporte que se tocó en el Home (para la pantalla "detalle")
    var folioAbierto by remember { mutableStateOf("") }

    // Cuando el login es exitoso, entra directo al Home
    LaunchedEffect(
        estadoLogin.loginExitoso,
        estadoLogin.esAnonimo
    ) {

        if (
            estadoLogin.loginExitoso ||
            estadoLogin.esAnonimo
        ) {
            pantallaActual = "home"
        }
    }

    // Cada vez que entramos al Home pedimos el historial a la base de datos
    LaunchedEffect(pantallaActual, estadoLogin.token) {
        if (pantallaActual == "home" && estadoLogin.token.isNotBlank()) {
            reporteVM.cargarHistorial(estadoLogin.token)
        }
    }

    // Crossfade: la pantalla anterior se desvanece mientras aparece la nueva,
    // en lugar de cambiar de golpe. Ojo: adentro se usa "pantalla", no "pantallaActual".
    Crossfade(
        targetState = pantallaActual,
        animationSpec = tween(DURACION_TRANSICION),
        label = "pantallas"
    ) { pantalla ->
        when (pantalla) {
            "login" -> PantallaLogin(
                vm = loginVM,
                alIrARegistro = { pantallaActual = "registro" },
                alEntrarAnonimo = {
                    loginVM.entrarAnonimo()
                    pantallaActual = "home"
                },
                modifier = modifier
            )
            "registro" -> PantallaRegistro(
                vm = registroVM,
                alIrALogin = { pantallaActual = "login" },
                modifier = modifier
            )
            "home" -> PantallaHome(
                reportes = if (estadoLogin.esAnonimo) {
                    emptyList()
                } else {
                    estadoReporte.historial
                },
                cargando = estadoReporte.cargandoHistorial,
                mensajeError = estadoReporte.errorHistorial,
                esAnonimo = estadoLogin.esAnonimo,
                alHacerReporte = {
                    reporteVM.nuevoReporte()
                    pantallaActual = "reporte"
                },
                alIrAPerfil = { pantallaActual = "perfil" },
                alAbrirReporte = { reporte ->
                    folioAbierto = reporte.folio ?: ""
                    reporteVM.cargarDetalle(estadoLogin.token, folioAbierto)
                    pantallaActual = "detalle"
                },
                modifier = modifier
            )
            "detalle" -> PantallaDetalleReporte(
                detalle = estadoReporte.detalle,
                cargando = estadoReporte.cargandoDetalle,
                mensajeError = estadoReporte.errorDetalle,
                alRegresar = { pantallaActual = "home" },
                alReintentar = { reporteVM.cargarDetalle(estadoLogin.token, folioAbierto) },
                modifier = modifier
            )

            "perfil" -> PantallaPerfil(
                nombre = estadoLogin.nombreUsuario,
                alRegresar = { pantallaActual = "home" },
                alCerrarSesion = {
                    loginVM.cerrarSesion()
                    reporteVM.limpiarTodo()
                    pantallaActual = "login"
                },
                modifier = modifier
            )
            "reporte" -> FlujoReporte(
                vm = reporteVM,
                token = estadoLogin.token,
                alSalir = { pantallaActual = "home" },
                modifier = modifier
            )
        }
    }
}

// Las 5 pantallas del reporte. El paso actual lo lleva el ReporteVM
@Composable
fun FlujoReporte(
    vm: ReporteVM,
    token: String,
    alSalir: () -> Unit,
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

    // Qué pantalla del reporte toca: 1 a 5 son los pasos, 6 es "Reporte enviado"
    val pantallaReporte = if (estado.folioGenerado.isNotEmpty()) 6 else estado.pasoActual

    // Mismo cambio suave entre los pasos del reporte
    Crossfade(
        targetState = pantallaReporte,
        animationSpec = tween(DURACION_TRANSICION),
        label = "pasos"
    ) { paso ->
        when (paso) {
            // Si ya tenemos folio, el reporte se envió bien.
            // El formulario se limpia al empezar el siguiente reporte (botón +),
            // así esta pantalla no se vacía mientras se desvanece.
            6 -> PantallaReporteEnviado(
                vm = vm,
                alCerrar = alSalir,
                modifier = modifier
            )
            1 -> PantallaLocacion(
                vm = vm,
                alRegresar = alSalir,   // en el paso 1 regresar es salir al Home
                modifier = modifier
            )
            2 -> PantallaFotos(
                vm = vm,
                alRegresar = { vm.pasoAnterior() },
                modifier = modifier
            )
            3 -> PantallaInfoNino(
                vm = vm,
                alRegresar = { vm.pasoAnterior() },
                modifier = modifier
            )
            4 -> PantallaDescripcion(
                vm = vm,
                alRegresar = { vm.pasoAnterior() },
                modifier = modifier
            )
            5 -> PantallaConfirmar(
                vm = vm,
                alRegresar = { vm.pasoAnterior() },
                alEnviar = { vm.enviar(token) },
                modifier = modifier
            )
        }
    }
}
