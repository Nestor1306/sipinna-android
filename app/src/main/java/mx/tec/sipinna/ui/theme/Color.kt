/*
 * SIPINNA — Color.kt
 * Capa: UI / Tema (colores y tipografía)
 *
 * Paleta de colores de la app, tomada de la identidad visual de SIPINNA.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 *   - Néstor Daniel Huidobro Aguilar
 * Creado: 2026-09-02
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * --- Paleta de la web (www.sipinna.com) ---
 * Son los mismos colores que usa la página, para que la app y la web se vean iguales.
 */
val AzulMarino = Color(0xFF102A43)        // encabezados y texto principal
val AzulInstitucional = Color(0xFF24658E) // enlaces destacados
val Morado = Color(0xFF65418D)            // estados "en proceso"
val Turquesa = Color(0xFF009CA6)          // detalles e íconos
val Magenta = Color(0xFFB21E70)           // detalles secundarios
val TextoPrincipal = AzulMarino           // azul casi negro
val TextoSecundario = Color(0xFF595959)   // gris oscuro

/** --- Colores SIPINNA (los nombres vienen del Figma; los valores, de la paleta de la web) --- */
val Teal = Color(0xFF007E87)            // botones principales, chips seleccionados
val TealOscuro = AzulMarino             // fin del degradado de botones
val ProgresoInicio = Color(0xFF007E87)  // barra de progreso (inicio)
val ProgresoFin = AzulInstitucional     // barra de progreso (fin)
val GrisBarra = Color(0xFFE5EDF0)       // parte no completada de la barra y bordes
val GrisTexto = TextoSecundario         // subtítulos
val GrisTextoClaro = Color(0xFF9F9F9F)  // descripción en tarjetas y textos de ejemplo
val GrisIcono = TextoSecundario         // placeholder de búsqueda
val SuperficieSecundaria = Color(0xFFE5EDF0) // fondo de buscador y cuadro de fotos
val SuperficiePrimaria = Color(0xFFF4F9FA)   // fondo alterno de pantallas
val FondoApp = Color(0xFFFFFFFF)
val AzulBoton = AzulInstitucional       // botón "+" del Home
val GrisBotonMenu = Color(0xFFE5EDF0)   // botón "≡" del Home
val AzulEditar = AzulInstitucional      // lápiz de editar en el perfil

/** Estados de reporte */
val VerdeConfirmado = Color(0xFF167247) // resuelto / éxito
val AmarilloProgreso = Color(0xFF9A5B00) // pendiente (ámbar oscuro)
val RojoNoApto = Color(0xFFB42332)      // rechazado / error
