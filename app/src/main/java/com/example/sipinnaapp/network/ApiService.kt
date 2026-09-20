package com.example.sipinnaapp.network

import com.example.sipinnaapp.model.LoginRequest
import com.example.sipinnaapp.model.LoginResponse
import com.example.sipinnaapp.model.ReporteRequest
import com.example.sipinnaapp.model.ReporteResponse
import com.example.sipinnaapp.model.UsuarioRegistro
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// Define los endpoints del backend Go
// Cada función aquí = un endpoint de la API
interface ApiService {

    // POST /user → registrar ciudadano
    @POST("user")
    suspend fun registrarUsuario(@Body usuario: UsuarioRegistro): Response<Any>

    // POST /login → iniciar sesión y obtener el token
    @POST("auth/login")
    suspend fun login(@Body credenciales: LoginRequest): Response<LoginResponse>

    // POST /reporte → crear un reporte (requiere el token del login)
    @POST("reporte")
    suspend fun crearReporte(
        @Header("Authorization") token: String,
        @Body reporte: ReporteRequest
    ): Response<ReporteResponse>
}
