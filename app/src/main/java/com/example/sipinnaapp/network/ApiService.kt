package com.example.sipinnaapp.network

import com.example.sipinnaapp.model.UsuarioRegistro
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// Define los endpoints del backend Go
// Cada función aquí = un endpoint de la API
interface ApiService {

    // POST /user → registrar ciudadano
    @POST("user")
    suspend fun registrarUsuario(@Body usuario: UsuarioRegistro): Response<Any>
}
