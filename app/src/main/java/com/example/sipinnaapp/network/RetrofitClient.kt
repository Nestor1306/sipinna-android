package com.example.sipinnaapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Configura la conexión con el servidor.
//
// USAR_SERVIDOR_LOCAL = true  → backend del equipo corriendo en tu Mac (puerto 8081)
//     Necesita: adb reverse tcp:8081 tcp:8081
//     (hace que "127.0.0.1" del emulador apunte a tu Mac)
// USAR_SERVIDOR_LOCAL = false → servidor del equipo en AWS
//
// Los dos corren el MISMO backend (rama main), así que solo cambia la dirección.
// Normalmente va en false (AWS). Ponlo en true solo para probar con tu Mac.
// Por ahora en true: AWS está apagado.
private const val USAR_SERVIDOR_LOCAL = true

private const val URL_LOCAL = "http://127.0.0.1:8081/"
private const val URL_AWS = "http://18.210.27.222:8080/"

object RetrofitClient {

    private val BASE_URL = if (USAR_SERVIDOR_LOCAL) URL_LOCAL else URL_AWS

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
