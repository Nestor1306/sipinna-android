package com.example.sipinnaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
    // Guardamos en qué pantalla estamos
    var pantallaActual by remember { mutableStateOf("login") }

    // Cuando el login es exitoso, entra directo al Home
    val estadoLogin by loginVM.estado.collectAsState()
    val estadoReporte by reporteVM.estado.collectAsState()
    LaunchedEffect(estadoLogin.loginExitoso) {
        if (estadoLogin.loginExitoso) {
            pantallaActual = "home"
        }
    }

    when (pantallaActual) {
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
            alHacerReporte = {
                reporteVM.nuevoReporte()
                pantallaActual = "reporte"
            },
            alIrAPerfil = { pantallaActual = "perfil" },
            modifier = modifier
        )
        "perfil" -> PantallaPerfil(
            nombre = estadoLogin.nombreUsuario,
            alRegresar = { pantallaActual = "home" },
            alCerrarSesion = {
                loginVM.cerrarSesion()
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

    // Si ya tenemos folio, el reporte se envió bien
    if (estado.folioGenerado.isNotEmpty()) {
        PantallaReporteEnviado(
            vm = vm,
            alCerrar = {
                vm.nuevoReporte()
                alSalir()
            },
            modifier = modifier
        )
        return
    }

    when (estado.pasoActual) {
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
