package com.example.sipinnaapp.network

import com.example.sipinnaapp.model.LoginRequest
import com.example.sipinnaapp.model.LoginResponse
import com.example.sipinnaapp.model.ReporteRequest
import com.example.sipinnaapp.model.ReporteResponse
import com.example.sipinnaapp.model.ReporteResumen
import com.example.sipinnaapp.model.UsuarioRegistro
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

// Define los endpoints del backend Go
// Cada función aquí = un endpoint de la API
interface ApiService {

    // POST /user → registrar ciudadano
    @POST("user")
    suspend fun registrarUsuario
                (@Body usuario: UsuarioRegistro
    ): Response<Any>

    // POST /login → iniciar sesión y obtener el token
    @POST("login")
    suspend fun login(
        @Body credenciales: LoginRequest
    ): Response<LoginResponse>

    // POST /reporte → crear un reporte (requiere el token del login)
    @POST("reporte")
    suspend fun crearReporte(
        @Header("Authorization") token: String?,
        @Body reporte: ReporteRequest
    ): Response<ReporteResponse>

    // POST /reporte/{id}/imagenes → sube UNA foto del reporte (se llama una vez por foto)
    // Se manda como formulario "multipart": el archivo va en "imagen" y su posición en "orden".
    // El backend la guarda en S3 y crea la fila en la tabla "imagenes_reporte".
    @Multipart
    @POST("reporte/{id}/imagenes")
    suspend fun subirImagen(
        @Header("Authorization") token: String?,
        @Path("id") reporteId: String,
        @Part imagen: MultipartBody.Part,
        @Part("orden") orden: RequestBody
    ): Response<Any>

    // GET /reporte/mis-reportes → los reportes del usuario que inició sesión
    // (el backend sabe quién es gracias al token)
    @GET("reporte/mis-reportes")
    suspend fun misReportes(
        @Header("Authorization") token: String
    ): Response<List<ReporteResumen>>
}
