/*
 * SIPINNA — BuscadorLugares.kt
 * Capa: Data (almacenamiento local en el teléfono)
 *
 * Búsqueda de direcciones con la Geocoding API de Mapbox para el paso de ubicación.
 *
 * Autores (según el historial de git):
 *   - Ángel Jesús Solorio García
 * Creado: 2026-10-14
 * Proyecto: Integración de seguridad informática en redes y sistemas de software
 *            Tecnológico de Monterrey, AGO–DIC 2026
 */
package mx.tec.sipinna.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Un resultado del buscador de lugares */
data class LugarSugerido(
    val nombre: String,      // "Av. Lago de Guadalupe 123"
    val direccion: String,   // dirección completa con colonia, ciudad y estado
    val latitud: Double,
    val longitud: Double
)

/**
 * Busca lugares por texto con la Geocoding API de Mapbox (v6).
 * Usa el mismo token del mapa. Se debe llamar desde un hilo de fondo (Dispatchers.IO).
 */
object BuscadorLugares {

    private const val URL_BASE = "https://api.mapbox.com/search/geocode/v6/forward"

    /**
     * Busca hasta 5 lugares en México que coincidan con [texto].
     *
     * Hace una petición HTTP bloqueante: llamar solo desde un hilo de fondo.
     * @param texto lo que escribió el usuario (calle, colonia o lugar).
     * @param token token público de Mapbox.
     * @param cercaLat latitud para dar prioridad a lugares cercanos, o null.
     * @param cercaLng longitud para dar prioridad a lugares cercanos, o null.
     * @return los lugares encontrados; lista vacía si no hay resultados o falló la red.
     */
    fun buscar(texto: String, token: String, cercaLat: Double?, cercaLng: Double?): List<LugarSugerido> {
        val parametros = buildString {
            append("q=").append(URLEncoder.encode(texto, "UTF-8"))
            append("&country=mx&language=es&limit=5&autocomplete=true")
            // Da prioridad a los lugares cercanos al pin actual
            if (cercaLat != null && cercaLng != null) append("&proximity=$cercaLng,$cercaLat")
            append("&access_token=").append(token)
        }

        val conexion = URL("$URL_BASE?$parametros").openConnection() as HttpURLConnection
        conexion.connectTimeout = 8000
        conexion.readTimeout = 8000

        try {
            if (conexion.responseCode != 200) return emptyList()

            val json = JSONObject(conexion.inputStream.bufferedReader().readText())
            val resultados = json.getJSONArray("features")

            return (0 until resultados.length()).map { i ->
                val lugar = resultados.getJSONObject(i)
                val datos = lugar.getJSONObject("properties")
                val coordenadas = lugar.getJSONObject("geometry").getJSONArray("coordinates")  // [lng, lat]
                LugarSugerido(
                    nombre = datos.optString("name"),
                    direccion = datos.optString("full_address", datos.optString("place_formatted")),
                    latitud = coordenadas.getDouble(1),
                    longitud = coordenadas.getDouble(0)
                )
            }
        } finally {
            conexion.disconnect()
        }
    }
}