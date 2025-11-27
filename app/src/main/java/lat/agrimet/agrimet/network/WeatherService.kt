package lat.agrimet.agrimet.network

import lat.agrimet.agrimet.model.WeatherPayload
import lat.agrimet.agrimet.model.Forecast
import lat.agrimet.agrimet.model.Alert
import lat.agrimet.agrimet.model.Alert.Severity
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class WeatherService(private val client: OkHttpClient, private val baseUrl: HttpUrl) {

    // --------------------------------------------------
    // 1. GET /api/v1/weather/current (Clima Actual)
    // --------------------------------------------------
    suspend fun getCurrentWeather(lat: Double, lon: Double): Result<WeatherPayload> {
        // ... (Implementación de la llamada getCurrentWeather) ...
        return try {
            val url = baseUrl.newBuilder()
                .addPathSegment("weather")
                .addPathSegment("current")
                .addQueryParameter("lat", lat.toString())
                .addQueryParameter("lon", lon.toString())
                .build()

            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Weather API Error: HTTP ${response.code}"))
                }

                val jsonString = response.body?.string() ?: "{}"
                val json = JSONObject(jsonString)

                // Conversión de números (Double/Int) a String para coincidir con WeatherPayload
                val payload = WeatherPayload(
                    temperatureC = json.getDouble("temperature").toString(),
                    condition = json.getString("condition"),
                    location = json.getString("location"),
                    humidityPct = json.getInt("humidity").toString(),
                    precipitationMm = json.getDouble("precipitation").toString(),
                    windKmh = json.getDouble("wind").toString(),
                    uvIndex = json.getInt("uvIndex").toString()
                )
                Result.success(payload)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --------------------------------------------------
    // 2. GET /api/v1/weather/forecast (Pronóstico)
    // --------------------------------------------------
    suspend fun getForecast(lat: Double, lon: Double): Result<List<Forecast>> {

        return try {
            val url = baseUrl.newBuilder()
                .addPathSegment("weather")
                .addPathSegment("forecast")
                .addQueryParameter("lat", lat.toString())
                .addQueryParameter("lon", lon.toString())
                .build()

            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Forecast API Error: HTTP ${response.code}"))
                }

                val jsonString = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(jsonString)
                val forecasts = mutableListOf<Forecast>()

                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    forecasts.add(Forecast(
                        day = json.getString("day"),
                        condition = json.getString("condition"),
                        highTemp = json.getInt("highTemp"),
                        lowTemp = json.getInt("lowTemp"),
                        iconName = json.getString("iconName")
                    ))
                }
                Result.success(forecasts)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --------------------------------------------------
    // 3. GET /api/v1/alerts (Alertas)
    // --------------------------------------------------
    suspend fun getAlerts(lat: Double, lon: Double): Result<List<Alert>> {
        // ... (Implementación de la llamada getAlerts) ...
        return try {
            val url = baseUrl.newBuilder()
                .addPathSegment("alerts")
                .addQueryParameter("lat", lat.toString())
                .addQueryParameter("lon", lon.toString())
                .build()

            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Alerts API Error: HTTP ${response.code}"))
                }

                val jsonString = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(jsonString)
                val alerts = mutableListOf<Alert>()

                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)

                    // Conversión de String de la API a nuestro Enum (Alert.Severity)
                    val severityString = json.getString("severity").uppercase()
                    val severityEnum = try {
                        Severity.valueOf(severityString)
                    } catch (e: IllegalArgumentException) {
                        Severity.INFO
                    }

                    alerts.add(Alert(
                        id = json.getString("id"),
                        title = json.getString("title"),
                        timestamp = json.getString("timestamp"),
                        body = json.getString("body"),
                        severity = severityEnum,
                        actionText = json.getString("actionText")
                    ))
                }
                Result.success(alerts)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}