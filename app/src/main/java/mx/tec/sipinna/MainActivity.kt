/*
 * SIPINNA — MainActivity.kt
 * Capa: Punto de entrada de la app
 *
 * Activity principal. Crea los ViewModels, programa los workers y controla la navegación
 * entre pantallas (AppNavegacion) y entre los 5 pasos del reporte (FlujoReporte).
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 *   - Néstor Daniel Huidobro Aguilar
 * Creado: 2026-09-02
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna

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
import mx.tec.sipinna.ui.theme.SipinnaAppTheme
import mx.tec.sipinna.view.PantallaConfirmar
import mx.tec.sipinna.view.PantallaDescripcion
import mx.tec.sipinna.view.PantallaFotos
import mx.tec.sipinna.view.PantallaHome
import mx.tec.sipinna.view.PantallaInfoNino
import mx.tec.sipinna.view.PantallaLocacion
import mx.tec.sipinna.view.PantallaLogin
import mx.tec.sipinna.view.PantallaPerfil
import mx.tec.sipinna.view.PantallaRegistro
import mx.tec.sipinna.view.PantallaReporteEnviado
import mx.tec.sipinna.viewmodel.LoginVM
import mx.tec.sipinna.viewmodel.RegistroVM
import mx.tec.sipinna.viewmodel.ReporteVM
import mx.tec.sipinna.view.PantallaDetalleReporte
import mx.tec.sipinna.worker.EnvioPendientesWorker
import mx.tec.sipinna.view.PantallaAvisoAnonimo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.activity.compose.BackHandler
import mx.tec.sipinna.view.PantallaRecuperar
import mx.tec.sipinna.viewmodel.RecuperarVM
import mx.tec.sipinna.view.PantallaCreditos
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import mx.tec.sipinna.worker.RevisarEstadosWorker
import mx.tec.sipinna.viewmodel.PerfilVM
import mx.tec.sipinna.view.PantallaEditarPerfil

/** Cuánto tarda el cambio suave entre pantallas (en milisegundos) */
const val DURACION_TRANSICION = 400
/** Cada cuánto se vuelve a pedir el historial mientras el Home está abierto (milisegundos) */
const val INTERVALO_ACTUALIZACION = 30_000L

/**
 * Única Activity de la app. Crea los ViewModels (que sobreviven a la rotación
 * de pantalla), programa los trabajos en segundo plano y muestra [AppNavegacion].
 */
class MainActivity : ComponentActivity() {

    private val registroVM: RegistroVM by viewModels()
    private val loginVM: LoginVM by viewModels()
    private val reporteVM: ReporteVM by viewModels()

    private val recuperarVM: RecuperarVM by viewModels()
    private val perfilVM: PerfilVM by viewModels()

    /**
     * Punto de entrada: programa [EnvioPendientesWorker] y [RevisarEstadosWorker]
     * y dibuja la app con el tema de SIPINNA.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Si quedaron reportes guardados sin internet, se programa su envío
        EnvioPendientesWorker.programar(applicationContext)

        // Revisa cada ~15 min si algún reporte cambió de estado
        RevisarEstadosWorker.programar(applicationContext)

        setContent {
            SipinnaAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavegacion(
                        registroVM = registroVM,
                        loginVM = loginVM,
                        reporteVM = reporteVM,
                        recuperarVM = recuperarVM,
                        perfilVM = perfilVM,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/**
 * Controla qué pantalla se muestra
 * "login", "registro", "home", "perfil" o "reporte"
 */
@Composable
fun AppNavegacion(
    registroVM: RegistroVM,
    loginVM: LoginVM,
    reporteVM: ReporteVM,
    recuperarVM: RecuperarVM,
    perfilVM: PerfilVM,
    modifier: Modifier = Modifier
) {
    val estadoLogin by loginVM.estado.collectAsState()
    val estadoReporte by reporteVM.estado.collectAsState()
    val estadoPerfil by perfilVM.estado.collectAsState()

    // Guardamos en qué pantalla estamos.
    // Si ya había una sesión guardada, empezamos directo en el Home.
    var pantallaActual by remember {
        mutableStateOf(
            if (estadoLogin.loginExitoso || estadoLogin.esAnonimo) "home" else "login"
        )
    }

    // Folio del reporte que se tocó en el Home (para la pantalla "detalle")
    var folioAbierto by remember { mutableStateOf("") }

    // A dónde regresar al salir de "Acerca de" (se abre desde el Login o desde el Perfil)
    var origenCreditos by remember { mutableStateOf("login") }

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

    // Mientras el Home esté en pantalla, el historial se pide ahora y luego cada 30 s,
    // así los cambios de estado aparecen solos. Si el usuario sale de la app se pausa,
    // y al volver se actualiza de inmediato.
    val cicloDeVida = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(pantallaActual, estadoLogin.token) {
        if (pantallaActual != "home") return@LaunchedEffect
        reporteVM.refrescarPendientes()
        if (estadoLogin.token.isBlank()) return@LaunchedEffect

        cicloDeVida.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                reporteVM.cargarHistorial(estadoLogin.token)
                delay(INTERVALO_ACTUALIZACION)
            }
        }
    }
    // Botón/gesto "atrás" de Android: regresa a la pantalla anterior en lugar de cerrar la app.
    // En el Home y el Login no se activa, así que ahí sí cierra la app (lo normal en Android).
    // El reporte maneja su propio "atrás" en FlujoReporte; el aviso anónimo también tiene el suyo.
    BackHandler(
        enabled = pantallaActual in listOf("detalle", "perfil", "editarPerfil", "registro", "recuperar", "creditos")
    ) {
        pantallaActual = when (pantallaActual) {
            "registro", "recuperar" -> "login"
            "creditos" -> origenCreditos
            "editarPerfil" -> "perfil"
            else -> "home"
        }
    }

    // Si el servidor rechazó la sesión (caducó), se cierra y se regresa al Login
    LaunchedEffect(estadoReporte.sesionVencida) {
        if (estadoReporte.sesionVencida) {
            loginVM.sesionExpirada()
            reporteVM.limpiarTodo()   // también reinicia sesionVencida
            pantallaActual = "login"
        }
    }

    // Lo mismo si la sesión caducó estando en "Mis datos"
    LaunchedEffect(estadoPerfil.sesionVencida) {
        if (estadoPerfil.sesionVencida) {
            loginVM.sesionExpirada()
            reporteVM.limpiarTodo()
            perfilVM.reiniciar()
            pantallaActual = "login"
        }
    }

    // Permiso de notificaciones (Android 13+): se pide al entrar con una cuenta.
    // Si ya lo dio, no aparece nada.
    val pedirPermisoNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(estadoLogin.token) {
        if (estadoLogin.token.isNotBlank() && Build.VERSION.SDK_INT >= 33) {
            pedirPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
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
                alEntrarAnonimo = { pantallaActual = "avisoAnonimo" },
                alOlvideContrasena = {
                    // Si ya había escrito su correo en el Login, se pasa para no escribirlo otra vez
                    recuperarVM.reiniciar(estadoLogin.email.trim())
                    pantallaActual = "recuperar"
                },
                alVerCreditos = {
                    origenCreditos = "login"
                    pantallaActual = "creditos"
                },
                modifier = modifier
            )
            "avisoAnonimo" -> PantallaAvisoAnonimo(
                alContinuarAnonimo = {
                    loginVM.entrarAnonimo()
                    pantallaActual = "home"
                },
                alCrearCuenta = { pantallaActual = "registro" },
                alRegresar = { pantallaActual = "login" },
                modifier = modifier
            )

            "registro" -> PantallaRegistro(
                vm = registroVM,
                alIrALogin = { pantallaActual = "login" },
                alRegistrado = { token, nombre ->
                    // Al quedar loginExitoso = true, el LaunchedEffect de arriba manda al Home
                    loginVM.entrarConSesion(token, nombre)
                },
                modifier = modifier
            )
            "recuperar" -> PantallaRecuperar(
                vm = recuperarVM,
                alRegresar = { pantallaActual = "login" },
                modifier = modifier
            )
            "creditos" -> PantallaCreditos(
                alRegresar = { pantallaActual = origenCreditos },
                modifier = modifier
            )
            "home" -> PantallaHome(
                // Arriba los que faltan por enviar, abajo los que ya están en el servidor
                reportes = estadoReporte.pendientes + if (estadoLogin.esAnonimo) {
                    emptyList()
                } else {
                    estadoReporte.historial
                },
                cargando = estadoReporte.cargandoHistorial,
                mensajeError = estadoReporte.errorHistorial,
                esAnonimo = estadoLogin.esAnonimo,
                avisoSinConexion = when {
                    estadoReporte.sinInternet ->
                        "Sin conexión a internet. Se muestran tus reportes guardados en el teléfono."
                    estadoReporte.historialSinConexion ->
                        "No se pudo conectar con el servidor. Se muestran tus reportes guardados."
                    else -> ""
                },
                alHacerReporte = {
                    reporteVM.nuevoReporte()
                    pantallaActual = "reporte"
                },
                alIrAPerfil = { pantallaActual = "perfil" },
                alVerCreditos = {
                    origenCreditos = "home"
                    pantallaActual = "creditos"
                },
                alAbrirReporte = { reporte ->
                    val idLocal = reporte.idLocal
                    if (idLocal != null) {
                        // Guardado sin internet: se abre con los datos del teléfono
                        folioAbierto = ""
                        reporteVM.abrirPendiente(idLocal)
                        pantallaActual = "detalle"
                    } else if (!reporte.folio.isNullOrBlank()) {
                        folioAbierto = reporte.folio
                        reporteVM.cargarDetalle(estadoLogin.token, folioAbierto)
                        pantallaActual = "detalle"
                    }
                },
                alRefrescar = {
                    reporteVM.refrescarPendientes()
                    if (estadoLogin.token.isNotBlank()) reporteVM.cargarHistorial(estadoLogin.token)
                },
                modifier = modifier
            )
            "detalle" -> PantallaDetalleReporte(
                detalle = estadoReporte.detalle,
                cargando = estadoReporte.cargandoDetalle,
                mensajeError = estadoReporte.errorDetalle,
                alRegresar = { pantallaActual = "home" },
                alReintentar = {
                    if (folioAbierto.isNotBlank()) reporteVM.cargarDetalle(estadoLogin.token, folioAbierto)
                },
                sinConexion = estadoReporte.detalleSinConexion,
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
                alEliminarCuenta = if (estadoLogin.esAnonimo) null else { alFallar ->
                    loginVM.eliminarCuenta(
                        alTerminar = {
                            reporteVM.limpiarTodo()
                            pantallaActual = "login"
                        },
                        alFallar = alFallar
                    )
                },
                modifier = modifier,
                alEditarDatos = if (estadoLogin.esAnonimo) null else {
                    {
                        perfilVM.cargar(estadoLogin.token)
                        pantallaActual = "editarPerfil"
                    }
                }
            )
            "editarPerfil" -> PantallaEditarPerfil(
                datos = estadoPerfil.datos,
                alCambiar = { perfilVM.cambiar(it) },
                alGuardar = {
                    perfilVM.guardar(estadoLogin.token) { nombre -> loginVM.actualizarNombre(nombre) }
                },
                alRegresar = { pantallaActual = "perfil" },
                modifier = modifier,
                cargando = estadoPerfil.cargando,
                guardando = estadoPerfil.guardando,
                error = estadoPerfil.error,
                guardado = estadoPerfil.guardado,
                puedeCambiarCorreo = estadoPerfil.puedeCambiarCorreo
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

/** Las 5 pantallas del reporte. El paso actual lo lleva el ReporteVM */
@Composable
fun FlujoReporte(
    vm: ReporteVM,
    token: String,
    alSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by vm.estado.collectAsState()

    // "Atrás" dentro del reporte: paso anterior (o a la Confirmación si se estaba editando).
    // En el paso 1 o en "Reporte enviado" regresa al Home. Mientras se envía no hace nada.
    BackHandler {
        when {
            estado.cargando -> Unit
            estado.folioGenerado.isNotEmpty() -> alSalir()
            estado.pasoActual > 1 || estado.editandoDesdeConfirmar -> vm.pasoAnterior()
            else -> alSalir()
        }
    }

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
