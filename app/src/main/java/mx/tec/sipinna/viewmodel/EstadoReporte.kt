/*
 * SIPINNA — EstadoReporte.kt
 * Capa: ViewModel (estado y lógica de las pantallas)
 *
 * Estado de todo el flujo de reporte, del historial y del detalle.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 *   - Néstor Daniel Huidobro Aguilar
 * Creado: 2026-09-13
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.viewmodel

import mx.tec.sipinna.model.ReporteResumen
import mx.tec.sipinna.model.ReporteDetalle
import mx.tec.sipinna.data.LugarSugerido

/** Un solo estado para todo el flujo del reporte (5 pasos) */
data class EstadoReporte(
    /** Paso 1: Locación */
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val direccion: String = "",
    val referencia: String = "",                // calle o punto de referencia (opcional)               // "si", "no", "no_se" ("" = sin contestar; es opcional)
    val buscandoUbicacion: Boolean = false,
    val sugerencias: List<LugarSugerido> = emptyList(),   // resultados del buscador de lugares

    /** Paso 2: Fotos (guardamos las Uri como texto, máximo 2) */
    val fotos: List<String> = emptyList(),

    /** Paso 3: Información del niño */
    val edadNino: String = "",
    val cantidadNinos: String = "",             // "1"…"4", "5 o más", "No sé" ("" = sin elegir)
    val peligroInmediato: Boolean? = null,      // null = todavía no contesta
    val tiposTrabajo: Set<String> = emptySet(),   // se pueden elegir varios
    val condicion: String = "",
    val riesgo: String = "",                    // "si", "no", "no_se" ("" = sin contestar; es opcional)

    /** Paso 4: Descripción */
    val descripcion: String = "",
    /** Cuándo vio el usuario la situación (milisegundos). null = "ahora" (al enviar) */
    val fechaAvistamiento: Long? = null,

    /** Control de pantalla */
    val pasoActual: Int = 1,        // 1 = locación ... 5 = confirmar
    val editandoDesdeConfirmar: Boolean = false, // se entró a un paso con "Editar" desde la Confirmación
    val cargando: Boolean = false,
    val error: String = "",
    val folioGenerado: String = "",   // si no está vacío, el reporte ya se envió
    val estadoGenerado: String = "",  // estado que devolvió el servidor al enviarlo
    val avisoFotos: String = "",      // si alguna foto no se pudo subir, aquí va el aviso
    val progresoEnvio: String = "",   // texto bajo el indicador de carga ("Subiendo foto 1 de 2…")

    /**
     * Progreso del envío (4 pasos). Se guarda para que, si algo falla a la mitad,
     * al tocar "Enviar reporte" otra vez se continúe donde se quedó en lugar de
     * crear un reporte duplicado.
     */
    val reporteIdBorrador: String = "",         // paso 1: id del reporte en DRAFT
    val tokenEdicion: String = "",          // paso 1: llave de un reporte anónimo para los pasos 2 a 4
    val folioBorrador: String = "",             // paso 1: folio que asignó el servidor
    val enviadoAnonimo: Boolean = false, // el reporte se envió sin cuenta (no tiene folio que consultar)
    val guardadoSinConexion: Boolean = false,   // no había internet: se guardó y se enviará solo después
    val idsImagenes: List<String> = emptyList(), // paso 2: id de cada foto, mismo orden que "fotos"
    val fotosSubidas: Set<String> = emptySet(),  // paso 3: ids de las fotos que ya subieron

    /** Reportes ya enviados (para las tarjetas del Home), vienen de la base de datos */
    val historial: List<ReporteResumen> = emptyList(),
    val cargandoHistorial: Boolean = false,
    val errorHistorial: String = "",
    val sesionVencida: Boolean = false,         // el servidor respondió 401: hay que volver a iniciar sesión
    val historialSinConexion: Boolean = false,  // el historial que se ve es la copia guardada (no hay internet)
    val sinInternet: Boolean = false,           // el teléfono no tiene red en este momento
    /** Reportes guardados en el teléfono que aún no se envían (ver ReportesPendientes) */
    val pendientes: List<ReporteResumen> = emptyList(),

    /** Detalle del reporte que se tocó en el Home (viene de GET /report/{folio}) */
    val detalle: ReporteDetalle? = null,
    val cargandoDetalle: Boolean = false,
    val errorDetalle: String = "",
    val detalleSinConexion: Boolean = false    // el detalle que se ve es una copia guardada
)

