package lat.agrimet.agrimet.network

// NotificationPoller.kt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.HttpUrl
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import android.util.Log
import org.json.JSONArray

// Modelo simple para la notificación recibida del servidor
data class ServerNotification(
    val id: String,
    val title: String,
    val body: String
)

// Función suspendida para consultar al servidor FastAPI
suspend fun pollNotification(
    userId: String,
    deviceId: String,
    apiKey: String
): ServerNotification? = withContext(Dispatchers.IO) {
    try {
        val url = HttpUrl.Builder()
            .scheme("http")
            .host("142.44.243.119")
            .port(8001)
            .addPathSegments("notify/poll")
            .addQueryParameter("user_id", userId)
            .addQueryParameter("device_id", deviceId)
            .build()

        val req = Request.Builder()
            .url(url)
            .header("X-API-Key", apiKey)
            .get()
            .build()

        HttpClient.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.w("PollNotif", "HTTP ${resp.code}: ${resp.message}")
                return@withContext null
            }

            val bodyStr = resp.body?.string().orEmpty()
            if (bodyStr.isBlank()) return@withContext null

            // bodyStr es un JSON Array: [...]
            val jsonArray = JSONArray(bodyStr)
            if (jsonArray.length() == 0) return@withContext null

            // Tomar el primer objeto del array
            val json = jsonArray.getJSONObject(jsonArray.length() - 1)

            val id = json.optString("id", "")
            val title = json.optString("title", "Notificación")
            val message = json.optString("message", json.optString("body", ""))

            if (message.isBlank()) null else ServerNotification(id, title, message)
        }
    } catch (e: SocketTimeoutException) {
        Log.w("PollNotif", "Timeout consultando servidor", e)
        null
    } catch (e: IOException) {
        Log.w("PollNotif", "Error de red consultando servidor", e)
        null
    } catch (e: Exception) {
        Log.e("PollNotif", "Error inesperado", e)
        null
    }
}
