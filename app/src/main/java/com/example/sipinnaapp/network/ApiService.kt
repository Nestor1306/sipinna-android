package com.example.sipinnaapp.network

import com.example.sipinnaapp.model.BorradorCreado
import com.example.sipinnaapp.model.ImagenesRegistradas
import com.example.sipinnaapp.model.LoginRequest
import com.example.sipinnaapp.model.LoginResponse
import com.example.sipinnaapp.model.RegistroImagenesRequest
import com.example.sipinnaapp.model.ReporteRequest
import com.example.sipinnaapp.model.RespuestaMisReportes
import com.example.sipinnaapp.model.RespuestaSubidaImagen
import com.example.sipinnaapp.model.UsuarioRegistro
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import com.example.sipinnaapp.model.RespuestaDetalle

// Define los endpoints del backend Go (rama main del equipo)
// Cada función aquí = un endpoint de la API.
// Las rutas que piden sesión reciben la cookie "session_token=..." (ver Sesion.kt)
interface ApiService {

    // POST /auth/citizen → registrar ciudadano
    @POST("auth/citizen")
    suspend fun registrarUsuario
                (@Body usuario: UsuarioRegistro
    ): Response<Any>

    // POST /auth/login → iniciar sesión (el token llega en la cookie)
    @POST("auth/login")
    suspend fun login(
        @Body credenciales: LoginRequest
    ): Response<LoginResponse>

    // ---- Envío de un reporte: 4 pasos (ver ReporteVM.enviar) ----

    // Paso 1. POST /report → crea el reporte como borrador (DRAFT)
    // y responde {"reporte_id": "..."}
    @POST("report")
    suspend fun crearReporte(
        @Header("Cookie") sesion: String?,
        @Body reporte: ReporteRequest
    ): Response<BorradorCreado>

    // Paso 2. POST /report/{id}/images → avisa cuántas fotos se van a subir.
    // El backend crea una fila "pendiente" por foto y responde sus IDs en el mismo orden:
    // {"success": ["id-foto-1", "id-foto-2"]}
    @POST("report/{report_id}/images")
    suspend fun registrarImagenes(
        @Header("Cookie") sesion: String?,
        @Path("report_id") reporteId: String,
        @Body imagenes: RegistroImagenesRequest
    ): Response<ImagenesRegistradas>

    // Paso 3. PUT /report/{id}/images/{image_id} → sube UNA foto (multipart, campo "file").
    // El backend la guarda en S3 y la marca como subida.
    @Multipart
    @PUT("report/{report_id}/images/{image_id}")
    suspend fun subirImagen(
        @Header("Cookie") sesion: String?,
        @Path("report_id") reporteId: String,
        @Path("image_id") imagenId: String,
        @Part archivo: MultipartBody.Part
    ): Response<RespuestaSubidaImagen>

    // Paso 4. PUT /report/{id}/submit → el reporte pasa de DRAFT a "registrado".
    // OJO: el backend solo lo acepta si el reporte tiene al menos una foto ya subida.
    @PUT("report/{report_id}/submit")
    suspend fun enviarReporte(
        @Header("Cookie") sesion: String?,
        @Path("report_id") reporteId: String
    ): Response<Any>

    // GET /report → los reportes del usuario que inició sesión
    // (el backend sabe quién es gracias a la cookie)
    @GET("report")
    suspend fun misReportes(
        @Header("Cookie") sesion: String
    ): Response<RespuestaMisReportes>

    // GET /report/{folio} → detalle de UNO de mis reportes.
    // Retrofit codifica el folio en la URL (acentos, espacios), no hay que hacerlo a mano.
    @GET("report/{folio}")
    suspend fun detalleReporte(
        @Header("Cookie") sesion: String,
        @Path("folio") folio: String
    ): Response<RespuestaDetalle>
}
