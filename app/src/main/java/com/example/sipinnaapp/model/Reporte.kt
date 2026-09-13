package com.example.sipinnaapp.model

// Lo que enviamos al servidor (POST /reporte)
// Los nombres coinciden con las columnas de la tabla "reportes"
data class ReporteRequest(
    val descripcion: String,
    val latitud: Double,
    val longitud: Double,
    val direccion: String,
    val cantidad_ninos: Int,
    val edad_ninos: String,
    val tipo_trabajo: String,          // varias opciones separadas por coma
    val horario_avistamiento: String,  // "2026-09-11T17:47"
    val condicion: String              // "Solo / Sola", "Con adultos", etc.
)

// Lo que el servidor devuelve al crear el reporte
data class ReporteResponse(
    val id: String,
    val folio: String,
    val estado: String
)

// Resumen que se muestra en las tarjetas del Home
data class ReporteResumen(
    val folio: String,
    val estado: String,       // "confirmado", "en_progreso", "no_apto"
    val descripcion: String,
    val direccion: String
)
