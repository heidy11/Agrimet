package lat.agrimet.agrimet.model.notifications

import com.google.gson.annotations.SerializedName

/**
 * ---------------------------------------------------------------------
 * 1. PETICIÓN: Registro de Token de FCM (POST /fcm/register)
 * Corresponde a la tabla user_devices.
 * ---------------------------------------------------------------------
 */
data class TokenRegistrationRequest(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("fcm_token") val fcmToken: String,
    @SerializedName("platform") val platform: String = "Android"
)


data class NotificationReportRequest(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("notification_id") val notificationId: String,
    @SerializedName("event_type") val eventType: String, // Ej: "OPENED", "DISMISSED"
    @SerializedName("event_timestamp") val eventTimestamp: Long
)

data class ApiResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)