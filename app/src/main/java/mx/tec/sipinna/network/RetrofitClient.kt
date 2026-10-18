/*
 * SIPINNA — RetrofitClient.kt
 * Capa: Network (comunicación con el backend)
 *
 * Configuración del cliente HTTP: URL del servidor y tiempos de espera.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 *   - Emiliano García Ramos
 *   - Néstor Daniel Huidobro Aguilar
 * Creado: 2026-09-02
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Configura la conexión con el servidor.
 *
 * USAR_SERVIDOR_LOCAL = true  → backend del equipo corriendo en tu Mac (puerto 8081)
 *     Necesita: adb reverse tcp:8081 tcp:8081
 *     (hace que "127.0.0.1" del emulador apunte a tu Mac)
 * USAR_SERVIDOR_LOCAL = false → servidor del equipo en AWS
 *
 * Los dos corren el MISMO backend (rama main), así que solo cambia la dirección.
 * Normalmente va en false (AWS). Ponlo en true solo para probar con tu Mac.
 */
private const val USAR_SERVIDOR_LOCAL = false

private const val URL_LOCAL = "http://127.0.0.1:8081/"
private const val URL_AWS = "https://api.sipinna.com/"

/**
 * Cliente HTTP único de la app (Retrofit + OkHttp + Gson).
 * Todas las llamadas al backend pasan por [api].
 */
object RetrofitClient {

    private val BASE_URL = if (USAR_SERVIDOR_LOCAL) URL_LOCAL else URL_AWS

    /**
     * Por defecto OkHttp espera solo 10 s. Con datos móviles lentos, subir una foto
     * tarda más que eso y el envío fallaría aunque todo esté bien (la web usa 60 s).
     */
    private val cliente = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Implementación de [ApiService]; se crea la primera vez que se usa. */
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
