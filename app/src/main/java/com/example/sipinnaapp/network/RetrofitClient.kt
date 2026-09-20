package com.example.sipinnaapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Configura la conexión con el servidor
// Funciona con: adb reverse tcp:8080 tcp:8080
// Ese comando hace que localhost del emulador apunte a tu Mac
object RetrofitClient {

    private const val BASE_URL = "http://192.168.1.119:3000/auth/citizen/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
