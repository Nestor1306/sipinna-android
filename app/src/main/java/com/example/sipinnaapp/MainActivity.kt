package com.example.sipinnaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.sipinnaapp.ui.theme.SipinnaAppTheme
import com.example.sipinnaapp.view.PantallaHome
import com.example.sipinnaapp.view.PantallaLogin
import com.example.sipinnaapp.view.PantallaRegistro
import com.example.sipinnaapp.viewmodel.LoginVM
import com.example.sipinnaapp.viewmodel.RegistroVM

class MainActivity : ComponentActivity() {

    private val registroVM: RegistroVM by viewModels()
    private val loginVM: LoginVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SipinnaAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavegacion(
                        registroVM = registroVM,
                        loginVM = loginVM,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Controla qué pantalla se muestra
// "login" o "registro"
@Composable
fun AppNavegacion(
    registroVM: RegistroVM,
    loginVM: LoginVM,
    modifier: Modifier = Modifier
) {
    // Guardamos en qué pantalla estamos
    var pantallaActual by remember { mutableStateOf("login") }

    // Cuando el login es exitoso, entra directo al Home
    val estadoLogin by loginVM.estado.collectAsState()
    LaunchedEffect(estadoLogin.loginExitoso) {
        if (estadoLogin.loginExitoso) {
            pantallaActual = "home"
        }
    }

    when (pantallaActual) {
        "login" -> PantallaLogin(
            vm = loginVM,
            alIrARegistro = { pantallaActual = "registro" },
            modifier = modifier
        )
        "registro" -> PantallaRegistro(
            vm = registroVM,
            alIrALogin = { pantallaActual = "login" },
            modifier = modifier
        )
        "home" -> PantallaHome(
            alCerrarSesion = {
                loginVM.cerrarSesion()
                pantallaActual = "login"
            },
            modifier = modifier
        )
    }
}
