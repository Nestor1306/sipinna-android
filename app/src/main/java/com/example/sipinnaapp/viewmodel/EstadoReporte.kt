package com.example.sipinnaapp.viewmodel

import com.example.sipinnaapp.model.ReporteResumen

// Un solo estado para todo el flujo del reporte (5 pasos)
data class EstadoReporte(
    // Paso 1: Locación
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val direccion: String = "",
    val buscandoUbicacion: Boolean = false,

    // Paso 2: Fotos (guardamos las Uri como texto)
    val fotos: List<String> = emptyList(),

    // Paso 3: Información del niño
    val edadNino: String = "",
    val tiposTrabajo: Set<String> = emptySet(),   // se pueden elegir varios
    val condicion: String = "",

    // Paso 4: Descripción
    val descripcion: String = "",

    // Control de pantalla
    val pasoActual: Int = 1,        // 1 = locación ... 5 = confirmar
    val cargando: Boolean = false,
    val error: String = "",
    val folioGenerado: String = "",   // si no está vacío, el reporte ya se envió

    // Reportes ya enviados (para las tarjetas del Home)
    val historial: List<ReporteResumen> = emptyList()
)
