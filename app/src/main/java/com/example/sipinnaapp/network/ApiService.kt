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
import com.example.sipinnaapp.model.CambioContrasena
import com.example.sipinnaapp.model.RespuestaMensaje
import com.example.sipinnaapp.model.SolicitudCodigo
import com.example.sipinnaapp.model.ConfirmacionEliminar
import retrofit2.http.HTTP
import com.example.sipinnaapp.model.PerfilActualizar
import com.example.sipinnaapp.model.PerfilCuenta

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


    // ---- Recuperar contraseña (solo con correo) ----

    // Manda un código de 6 dígitos al correo (vence en 10 min, uno por minuto)
    @POST("auth/password/forgot")
    suspend fun pedirCodigo(
        @Body datos: SolicitudCodigo
    ): Response<RespuestaMensaje>

    // Cambia la contraseña con el código. No inicia sesión.
    @POST("auth/password/reset")
    suspend fun cambiarContrasena(
        @Body datos: CambioContrasena
    ): Response<RespuestaMensaje>


    // DELETE /account → elimina la cuenta de quien tiene la sesión.
    // Retrofit no deja mandar cuerpo con @DELETE, por eso se usa @HTTP.
    @HTTP(method = "DELETE", path = "account", hasBody = true)
    suspend fun eliminarCuenta(
        @Header("Cookie") sesion: String,
        @Body confirmacion: ConfirmacionEliminar
    ): Response<Any>

    // GET /account → datos de la cuenta de quien tiene la sesión
    @GET("account")
    suspend fun miCuenta(
        @Header("Cookie") sesion: String
    ): Response<PerfilCuenta>

    // PUT /account → guarda los cambios y regresa los datos ya guardados
    @PUT("account")
    suspend fun actualizarCuenta(
        @Header("Cookie") sesion: String,
        @Body datos: PerfilActualizar
    ): Response<PerfilCuenta>

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
        @Header("X-Edit-Token") tokenEdicion: String?,
        @Path("report_id") reporteId: String,
        @Body imagenes: RegistroImagenesRequest
    ): Response<ImagenesRegistradas>

    // Paso 3. PUT /report/{id}/images/{image_id} → sube UNA foto (multipart, campo "file").
    // El backend la guarda en S3 y la marca como subida.
    @Multipart
    @PUT("report/{report_id}/images/{image_id}")
    suspend fun subirImagen(
        @Header("Cookie") sesion: String?,
        @Header("X-Edit-Token") tokenEdicion: String?,
        @Path("report_id") reporteId: String,
        @Path("image_id") imagenId: String,
        @Part archivo: MultipartBody.Part
    ): Response<RespuestaSubidaImagen>

    // Paso 4. PUT /report/{id}/submit → el reporte pasa de DRAFT a "registrado".
    @PUT("report/{report_id}/submit")
    suspend fun enviarReporte(
        @Header("Cookie") sesion: String?,
        @Header("X-Edit-Token") tokenEdicion: String?,
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
