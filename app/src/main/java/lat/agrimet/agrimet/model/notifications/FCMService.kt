package lat.agrimet.agrimet.notificacion

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import lat.agrimet.agrimet.model.notifications.TokenRegistrationRequest
import lat.agrimet.agrimet.network.HttpClient
import lat.agrimet.agrimet.network.NotificationManagementService
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.UUID

/**
 * Servicio que extiende FirebaseMessagingService para manejar los tokens de registro
 * y la recepción de notificaciones FCM.
 */
class FCMService : FirebaseMessagingService() {

    private val TAG = "AgrimetFCMService"

    // CAMBIAR LA URL
    private val BASE_URL = "http://142.44.243.119:8000/api/v1".toHttpUrl()

    // Usamos el NotificationManagementService que ya creamos
    private val notificationService by lazy {
        NotificationManagementService(HttpClient.client, BASE_URL)
    }

    // Configuración para Corrutinas en el servicio
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    // Constantes de SharedPreferences (DEBEN COINCIDIR CON MainActivity)
    private val PREFS_NAME = "AgrimetUserPrefs"
    private val PREF_USER_ID = "anonymous_user_id"


    /**
     * Llamado cuando se genera un nuevo token de registro o se refresca.
     * Este es el punto clave para enviar el token a FastAPI.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Nuevo Token: $token")

        // ⭐️ LÓGICA INTEGRADA ⭐️
        // El servicio obtiene el ID persistente y lo usa para registrar el token.
        sendRegistrationToServer(token)
    }

    /**
     * Lógica para obtener el ID persistente y enviar el token al backend.
     */
    private fun sendRegistrationToServer(fcmToken: String) {
        // Obtenemos el ID persistente. Si la Activity no se ha ejecutado, puede ser 0L,
        // pero la Activity es la única responsable de crearlo. Asumimos que la App se abre primero.
        val userId = getPersistentUserId()

        if (userId == 0L) {
            Log.e(TAG, "ID de usuario persistente no encontrado (0L). El token no será enviado. Asegura que MainActivity se ejecute primero.")
            return
        }

        serviceScope.launch {
            val request = TokenRegistrationRequest(
                userId = userId, // ⭐️ USAMOS EL ID OBTENIDO DE PREFERENCES ⭐️
                fcmToken = fcmToken,
                platform = "Android"
            )

            val result = notificationService.registerToken(request)

            result.onSuccess {
                Log.i(TAG, "Registro de token exitoso en FastAPI para ID: $userId")
            }.onFailure { error ->
                Log.e(TAG, "Fallo al registrar token en FastAPI: ${error.message}", error)
            }
        }
    }

    /**
     * Función que lee el ID persistente (creado por MainActivity).
     */
    private fun getPersistentUserId(): Long {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Si no existe, devuelve 0L (la Activity es la que debe generar el UUID)
        return prefs.getLong(PREF_USER_ID, 0L)
    }

    /**
     * Llamado cuando se recibe un mensaje FCM.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Mensaje recibido de: ${remoteMessage.from}")

        // Revisa si el mensaje contiene un payload de datos (enviado por FastAPI)
        if (remoteMessage.data.isNotEmpty()) {
            val data = remoteMessage.data

            val notificationId = data["notification_id"] ?: run {
                Log.e(TAG, "Mensaje sin notification_id. No se puede rastrear.")
                return
            }
            val title = data["title"] ?: "Alerta Agrimet"
            val body = data["body"] ?: "Nuevo mensaje del sistema."

            // LLAMADA AL NOTIHELPER para mostrar y configurar el Intent de reporte
            val notiHelper = NotiHelper(applicationContext)
            notiHelper.showFCMNotification(
                notificationId = notificationId,
                title = title,
                body = body
            )

            Log.i(TAG, "Notificación local mostrada para ID: $notificationId")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // Cancela todas las corrutinas cuando el servicio es destruido
    }
}