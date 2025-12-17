package lat.agrimet.agrimet.network

import lat.agrimet.agrimet.model.notifications.TokenRegistrationRequest
import lat.agrimet.agrimet.model.notifications.NotificationReportRequest
import lat.agrimet.agrimet.model.notifications.ApiResponse
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Servicio dedicado a la gestión de tokens de Firebase (FCM) y al reporte de eventos
 * de notificaciones a los nuevos endpoints de FastAPI.
 * Usa Gson para la serialización/deserialización y se ejecuta en Dispatchers.IO.
 */
class NotificationManagementService(private val client: OkHttpClient, private val baseUrl: HttpUrl) {

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // ----------------------------------------------------------------------------------
    // 1. POST /api/v1/fcm/register (Registro de Tokens)
    // Corresponde a la tabla user_devices
    // ----------------------------------------------------------------------------------
    suspend fun registerToken(request: TokenRegistrationRequest): Result<ApiResponse> = withContext(Dispatchers.IO) {
        try {
            val url = baseUrl.newBuilder()
                .addPathSegment("fcm")
                .addPathSegment("register")
                .build()

            val jsonBody = gson.toJson(request)
            val body = jsonBody.toRequestBody(jsonMediaType)

            val httpRequest = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e("NotifService", "Error al registrar token: HTTP ${response.code}")
                    return@withContext Result.failure(IOException("Token Registration Error: HTTP ${response.code}. Body: $responseBody"))
                }

                val type = object : TypeToken<ApiResponse>() {}.type
                val apiResponse = gson.fromJson<ApiResponse>(responseBody, type)

                Result.success(apiResponse)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------------------------------------------------------------------------
    // 2. POST /api/v1/notification/report (Reporte de Eventos de Interacción)
    // Corresponde a las tablas notification_events y notification_user
    // ----------------------------------------------------------------------------------
    suspend fun reportEvent(request: NotificationReportRequest): Result<ApiResponse> = withContext(Dispatchers.IO) {
        try {
            val url = baseUrl.newBuilder()
                .addPathSegment("notification")
                .addPathSegment("report")
                .build()

            val jsonBody = gson.toJson(request)
            val body = jsonBody.toRequestBody(jsonMediaType)

            val httpRequest = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e("NotifService", "Error al reportar evento: HTTP ${response.code}")
                    return@withContext Result.failure(IOException("Event Report Error: HTTP ${response.code}. Body: $responseBody"))
                }

                val type = object : TypeToken<ApiResponse>() {}.type
                val apiResponse = gson.fromJson<ApiResponse>(responseBody, type)

                Result.success(apiResponse)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}