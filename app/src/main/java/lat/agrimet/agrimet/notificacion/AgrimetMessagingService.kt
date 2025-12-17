package lat.agrimet.agrimet.notification

import android.util.Log
import androidx.lifecycle.lifecycleScope
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

/**
 * Servicio que extiende FirebaseMessagingService para manejar los tokens de registro
 * y la recepción de notificaciones FCM.
 */
class AgrimetMessagingService : FirebaseMessagingService() {

    // 🛑 CONFIGURACIÓN DE RED 🛑
    // Debe coincidir con la URL BASE donde está montado el router de FastAPI (Módulo 2)
    private val BASE_URL = "http://142.44.243.119:9000/api/v1".toHttpUrl()

    // Usamos el NotificationManagementService que acabamos de crear
    private val notificationService by lazy {
        NotificationManagementService(HttpClient.client, BASE_URL)
    }

    // Usamos un Scope para Corrutinas para tareas de larga duración (como llamadas a la API)
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)


    /**
     * Llamado cuando se genera un nuevo token de registro o se refresca.
     * Este es el punto clave para enviar el token a FastAPI.
     */
    override fun onNewToken(token: String) {
        Log.d("FCM", "Nuevo Token: $token")

        // ⚠️ Nota: Reemplaza 12345L con el ID de usuario real de tu sistema
        // (Esto se debería obtener del sistema de autenticación de tu app)
        sendRegistrationToServer(userId = 12345L, fcmToken = token)
    }

    /**
     * Lógica para enviar el token al backend usando el NotificationManagementService.
     */
    private fun sendRegistrationToServer(userId: Long, fcmToken: String) {
        serviceScope.launch {
            val request = TokenRegistrationRequest(
                userId = userId,
                fcmToken = fcmToken,
                platform = "Android"
            )

            val result = notificationService.registerToken(request)

            result.onSuccess {
                Log.i("FCM", "Registro de token exitoso en FastAPI.")
            }.onFailure { error ->
                Log.e("FCM", "Fallo al registrar token en FastAPI: ${error.message}", error)
                // Aquí podrías implementar una lógica de reintento si es necesario
            }
        }
    }

    /**
     * Llamado cuando se recibe un mensaje FCM (mientras la app está en foreground o background).
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Mensaje recibido de: ${remoteMessage.from}")

        // Revisa si el mensaje contiene un payload de datos (enviado por FastAPI)
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Payload de Datos: ${remoteMessage.data}")

            // 💡 Lógica pendiente: Procesar remoteMessage.data para mostrar la notificación
            // Aquí se llamaría a NotiHelper para construir y mostrar la notificación

            val notificationId = remoteMessage.data["notification_id"] ?: return
            val title = remoteMessage.data["title"] ?: "Alerta Agrimet"
            val body = remoteMessage.data["body"] ?: "Nuevo mensaje del sistema."

            // La lógica para mostrar la notificación y configurar el PendingIntent para el
            // reporte de eventos se implementará en el NotiHelper y el MainActivity.

        }

        // Revisa si el mensaje contiene una notificación (usado por Firebase Console)
        remoteMessage.notification?.let {
            Log.d("FCM", "Cuerpo de Notificación: ${it.body}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // Cancela todas las corrutinas cuando el servicio es destruido
    }
}