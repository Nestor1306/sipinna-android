package com.example.sipinnaapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Configura la conexión con el servidor
// 10.0.2.2 = localhost de tu Mac visto desde el emulador Android
object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
