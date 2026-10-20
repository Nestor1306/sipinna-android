package mx.tec.sipinna

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import mx.tec.sipinna.view.estadoPublico
import mx.tec.sipinna.viewmodel.EstadoReporte
import mx.tec.sipinna.viewmodel.RegistroVM
import mx.tec.sipinna.viewmodel.ReporteVM
import mx.tec.sipinna.viewmodel.contrasenaValida
import org.junit.Test

// Pruebas unitarias de la Etapa 4 (casos CP del plan de pruebas).
// Solo prueban reglas que no necesitan el celular ni el servidor.
class PruebasEtapa4Test {

    // ---- CP-REG-03: contraseña, valores límite (10 mínimo, 72 bytes máximo) ----

    @Test
    fun cpReg03_contrasenaLimites() {
        assertFalse(contrasenaValida("Abcdefgh!"))            // 9 caracteres: rechaza
        assertTrue(contrasenaValida("Abcdefghi!"))            // 10 caracteres: acepta
        assertTrue(contrasenaValida("A!" + "a".repeat(70)))   // 72 bytes: acepta
        assertFalse(contrasenaValida("A!" + "a".repeat(71)))  // 73 bytes: rechaza
        assertFalse(contrasenaValida("A!" + "ñ".repeat(36)))  // 74 bytes (la ñ cuenta 2): rechaza
    }

    // ---- CP-REG-04: formato de correo (partición de equivalencia) ----

    @Test
    fun cpReg04_correo() {
        assertTrue(RegistroVM.correoValido("ana@ejemplo.com"))
        assertTrue(RegistroVM.correoValido("  ana@ejemplo.com  "))  // espacios al inicio y final se ignoran
        assertFalse(RegistroVM.correoValido("ana@ejemplo"))          // sin extensión
        assertFalse(RegistroVM.correoValido("anaejemplo.com"))       // sin @
        assertFalse(RegistroVM.correoValido(""))                     // vacío
    }

    // ---- CP-REG-05: teléfono de 10 dígitos (valores límite 9, 10, 11) ----

    @Test
    fun cpReg05_telefono() {
        assertFalse(RegistroVM.telefonoValido("551234567"))     // 9
        assertTrue(RegistroVM.telefonoValido("5512345678"))     // 10
        assertFalse(RegistroVM.telefonoValido("55123456789"))   // 11
    }

    // ---- CP-REP-01: no se avanza sin ubicación ----

    @Test
    fun cpRep01_pasoUbicacion() {
        assertEquals("Indica la ubicación del reporte", ReporteVM.errorDelPaso(EstadoReporte(pasoActual = 1)))
        assertEquals("", ReporteVM.errorDelPaso(EstadoReporte(pasoActual = 1, latitud = 19.56, longitud = -99.27)))
    }

    // ---- CP-REP-04: preguntas obligatorias del NNA (una por una) ----

    @Test
    fun cpRep04_infoNino() {
        val completo = EstadoReporte(
            pasoActual = 3,
            peligroInmediato = false,
            cantidadNinos = "2",
            edadNino = "12 a 14 años",
            tiposTrabajo = setOf("Venta ambulante"),
            condicion = "Con adultos"
        )
        assertEquals("", ReporteVM.errorDelPaso(completo))
        assertEquals("Indica si hay peligro inmediato", ReporteVM.errorDelPaso(completo.copy(peligroInmediato = null)))
        assertEquals("Indica cuántos niños viste", ReporteVM.errorDelPaso(completo.copy(cantidadNinos = "")))
        assertEquals("Selecciona la edad aproximada", ReporteVM.errorDelPaso(completo.copy(edadNino = "")))
        assertEquals("Selecciona al menos un tipo de trabajo", ReporteVM.errorDelPaso(completo.copy(tiposTrabajo = emptySet())))
        assertEquals("Selecciona en qué condición se encuentra", ReporteVM.errorDelPaso(completo.copy(condicion = "")))
    }

    // ---- CP-REP-05: cantidad de niños que se manda al servidor ----

    @Test
    fun cpRep05_cantidadParaServidor() {
        assertEquals(1, ReporteVM.cantidadParaServidor("1"))
        assertEquals(4, ReporteVM.cantidadParaServidor("4"))
        assertEquals(5, ReporteVM.cantidadParaServidor("5 o más"))
        assertNull(ReporteVM.cantidadParaServidor("No sé"))
        assertNull(ReporteVM.cantidadParaServidor(""))
    }

    // ---- CP-REP-06: descripción de mínimo 10 palabras (valores límite 9, 10, 11) ----

    @Test
    fun cpRep06_descripcionMinimo() {
        val nueve = "uno dos tres cuatro cinco seis siete ocho nueve"
        val diez = "$nueve diez"
        val once = "$diez once"
        assertEquals(9, ReporteVM.contarPalabras(nueve))
        assertEquals(2, ReporteVM.contarPalabras("   hola    mundo  "))   // espacios de más no cuentan
        assertEquals(0, ReporteVM.contarPalabras(""))
        assertTrue(ReporteVM.errorDelPaso(EstadoReporte(pasoActual = 4, descripcion = nueve)).isNotEmpty())
        assertEquals("", ReporteVM.errorDelPaso(EstadoReporte(pasoActual = 4, descripcion = diez)))
        assertEquals("", ReporteVM.errorDelPaso(EstadoReporte(pasoActual = 4, descripcion = once)))
    }

    // ---- CP-SEG-01: traducción de estados internos a estados públicos ----

    @Test
    fun cpSeg01_estadosPublicos() {
        assertEquals("recibido", estadoPublico("registrado"))
        assertEquals("recibido", estadoPublico(null))
        assertEquals("en_revision", estadoPublico("en_revision"))
        assertEquals("en_revision", estadoPublico("duplicado"))
        assertEquals("en_revision", estadoPublico("estado_nuevo"))   // un estado desconocido no rompe la app
        assertEquals("canalizado", estadoPublico("canalizado"))
        assertEquals("en_atencion", estadoPublico("en_seguimiento"))
        assertEquals("concluido", estadoPublico("archivado"))
        assertEquals("concluido", estadoPublico("cancelado"))
        assertEquals("borrador", estadoPublico("DRAFT"))
        assertEquals("pendiente_envio", estadoPublico("pendiente_envio"))
    }
}
