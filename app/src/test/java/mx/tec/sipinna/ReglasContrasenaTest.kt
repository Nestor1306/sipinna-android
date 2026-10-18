package mx.tec.sipinna

import mx.tec.sipinna.viewmodel.contrasenaValida
import mx.tec.sipinna.viewmodel.errorDeContrasena
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test


class ReglasContrasenaTest {

    @Test
    fun contrasenasValidas() {
        assertTrue(contrasenaValida("Sipinna2026!"))
        assertTrue(contrasenaValida("Mi casa es azul #3"))     // con espacios
        assertTrue(contrasenaValida("Ñandú_feliz"))            // mayúscula con acento/ñ
        assertTrue(contrasenaValida("A!" + "a".repeat(70)))    // exactamente 72
    }

    @Test
    fun muyCorta() {
        assertFalse(contrasenaValida("Corta!1"))
        assertEquals("La contraseña debe tener al menos 10 caracteres",
            errorDeContrasena("Corta!1")
        )
    }

    @Test
    fun muyLarga() {
        assertFalse(contrasenaValida("A!" + "a".repeat(127)))  // 129
    }

    @Test
    fun sinMayuscula() {
        assertFalse(contrasenaValida("sipinna2026!"))
        assertEquals("La contraseña debe tener al menos una letra mayúscula",
            errorDeContrasena("sipinna2026!")
        )
    }

    @Test
    fun sinEspecial() {
        assertFalse(contrasenaValida("Sipinna2026"))
        assertFalse(contrasenaValida("Sipinna 2026"))  // el espacio no cuenta como especial
    }

    @Test
    fun noPermiteSaltosDeLinea() {
        assertFalse(contrasenaValida("Sipinna\n2026!"))
    }

    @Test
    fun valida_sinError() {
        assertNull(errorDeContrasena("Sipinna2026!"))
    }
}