package mx.tec.sipinna.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Un resultado del buscador de lugares
data class LugarSugerido(
    val nombre: String,      // "Av. Lago de Guadalupe 123"
    val direccion: String,   // dirección completa con colonia, ciudad y estado
    val latitud: Double,
    val longitud: Double
)

// Busca lugares por texto con la Geocoding API de Mapbox (v6).
// Usa el mismo token del mapa. Se debe llamar desde un hilo de fondo (Dispatchers.IO).
object BuscadorLugares {

    private const val URL_BASE = "https://api.mapbox.com/search/geocode/v6/forward"

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