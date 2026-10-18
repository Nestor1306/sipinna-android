/*
 * SIPINNA — Theme.kt
 * Capa: UI / Tema (colores y tipografía)
 *
 * Tema de Material 3 con los colores de SIPINNA (siempre en modo claro).
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 *   - Néstor Daniel Huidobro Aguilar
 * Creado: 2026-09-02
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Colores que usan los componentes de Material (campos de texto, diálogos, cargando, etc.)
 * cuando la pantalla no les pone un color propio. Salen de la paleta de la web.
 */
private val ColoresSipinna = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    secondary = AzulInstitucional,
    onSecondary = Color.White,
    tertiary = Morado,
    onTertiary = Color.White,
    background = FondoApp,
    onBackground = TextoPrincipal,
    surface = FondoApp,
    onSurface = TextoPrincipal,
    onSurfaceVariant = TextoSecundario,
    error = RojoNoApto,
    onError = Color.White
)

/**
 * La app siempre usa estos colores. Antes tomaba los del fondo de pantalla del celular
 * (Android 12+) y en modo oscuro algunos textos se perdían sobre los fondos claros.
 */
@Composable
fun SipinnaAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColoresSipinna,
        typography = Typography,
        content = content
    )
}
