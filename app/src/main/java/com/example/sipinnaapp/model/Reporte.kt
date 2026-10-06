package com.example.sipinnaapp.model

import com.google.gson.annotations.SerializedName

// Lo que enviamos al servidor (POST /report).
// Los nombres coinciden con el JSON que espera el backend (en inglés).
// La dirección escrita y la condición del niño no tienen columna en la base,
// por eso ReporteVM las agrega al final de "description".
data class ReporteRequest(
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val children_quantity: Int,
    val children_age: String,
    val work_type: String,             // varias opciones separadas por coma
    val sighting_time: String          // "2026-09-11T17:47"
)

// Paso 1: respuesta de POST /report → {"reporte_id": "..."}
data class BorradorCreado(
    @SerializedName("reporte_id") val reporteId: String?
)

// Paso 2: lo que mandamos a POST /report/{id}/images
// {"images": [{"file_name": "foto1.jpg", "content_type": "image/jpeg"}, ...]}
data class RegistroImagenesRequest(
    @SerializedName("images") val imagenes: List<ImagenPorSubir>
)

data class ImagenPorSubir(
    @SerializedName("file_name") val nombreArchivo: String,
    @SerializedName("content_type") val tipo: String
)

// Paso 2: respuesta → {"success": ["id-foto-1", "id-foto-2"]} (mismo orden que enviamos)
data class ImagenesRegistradas(
    @SerializedName("success") val ids: List<String>?
)

// Paso 3: respuesta de PUT /report/{id}/images/{image_id}
// {"success": true, "status": "uploaded" | "already_uploaded" | ..., "image_id": "...", "message": "..."}
data class RespuestaSubidaImagen(
    val success: Boolean?,
    val status: String?,
    val message: String?
)

// Lo que devuelve GET /report: {"reports": [ ... ]}
data class RespuestaMisReportes(
    val reports: List<ReporteResumen>?
)

// Resumen de cada reporte, para las tarjetas del Home
data class ReporteResumen(
    val folio: String?,
    val report_state: String?,   // "DRAFT", "registrado", "en_revision", "canalizado"...
    val latitude: Double?,
    val longitude: Double?,
    val description: String?
)

// Lo que devuelve GET /report/{folio}: {"report": { ... }}
data class RespuestaDetalle(
    val report: ReporteDetalle?
)

// Detalle completo de un reporte, tal como está en la base de datos
data class ReporteDetalle(
    val folio: String?,
    val description: String?,        // incluye al final "Dirección: ..." y "Condición: ..."
    val latitude: Double?,
    val longitude: Double?,
    val children_quantity: Int?,
    val children_age: String?,
    val work_type: String?,          // varias opciones separadas por coma
    val created_at: String?,
    val sighting_time: String?,
    val zone_name: String?,
    val last_state: String?,         // estado actual: "registrado", "en_revision"...
    val state_changed_at: String?,
    val images: List<String>?        // claves de las fotos en S3
)