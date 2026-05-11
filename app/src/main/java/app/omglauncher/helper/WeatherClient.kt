package app.omglauncher.helper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

object WeatherClient {

    data class WeatherResult(val temperature: Int, val description: String) {
        fun format(): String = "$temperature° $description"
    }

    suspend fun fetchWeather(): WeatherResult? = withContext(Dispatchers.IO) {
        try {
            val (lat, lon) = fetchLocation() ?: return@withContext null
            fetchCurrentWeather(lat, lon)
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchLocation(): Pair<Double, Double>? {
        val connection = URL("https://ipinfo.io/json").openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        try {
            val response = readResponse(connection) ?: return null
            val json = JSONObject(response)
            val loc = json.optString("loc", "").split(",")
            if (loc.size != 2) return null
            return Pair(loc[0].toDouble(), loc[1].toDouble())
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchCurrentWeather(lat: Double, lon: Double): WeatherResult? {
        val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        try {
            val response = readResponse(connection) ?: return null
            val current = JSONObject(response).optJSONObject("current") ?: return null
            val temp = current.optDouble("temperature_2m", Double.NaN)
            val code = current.optInt("weather_code", -1)
            if (temp.isNaN()) return null
            return WeatherResult(temp.toInt(), weatherCodeToDescription(code))
        } finally {
            connection.disconnect()
        }
    }

    private fun readResponse(connection: HttpURLConnection): String? {
        if (connection.responseCode !in 200..299) return null
        return connection.inputStream?.use { input ->
            Scanner(input).use { scanner ->
                scanner.useDelimiter("\\A")
                if (scanner.hasNext()) scanner.next() else null
            }
        }
    }

    private fun weatherCodeToDescription(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75 -> "Snow"
        77 -> "Snow grains"
        80, 81, 82 -> "Showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Hail"
        else -> "Unknown"
    }
}
