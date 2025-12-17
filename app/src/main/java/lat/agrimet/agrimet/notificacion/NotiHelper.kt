package lat.agrimet.agrimet.notificacion

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import lat.agrimet.agrimet.MainActivity
import lat.agrimet.agrimet.R
//alertas locales
import lat.agrimet.agrimet.model.Alert


// ⭐️ CONSTANTES PARA EL REPORTE DE EVENTOS (MÓDULO 2) ⭐️
object NotificationConstants {
    const val EXTRA_NOTIFICATION_ID = "notification_id" // Clave para el ID de la notificación
    const val EXTRA_EVENT_TYPE = "event_type"         // Clave para el tipo de evento
    // Tipos de Eventos (Coinciden con el contrato de FastAPI/BD)
    const val EVENT_OPENED = "OPENED"
    const val EVENT_DISMISSED = "DISMISSED"
    const val EVENT_ACTION_TAKEN = "ACTION_TAKEN"
}

// ⭐️ OBJETO DE CONFIGURACIÓN DEL CANAL (REQUERIDO) ⭐️
object NotiConfig {
    const val CHANNEL_ID = "agrimet_alerts_channel"
    const val NOTIFICATION_REQUEST_CODE = 100
}


class NotiHelper(private val context: Context) {
    private val nm = NotificationManagerCompat.from(context)

    fun sendBigPictureWithAction(title: String, message: String, id: Int = 300) { //Codigo de error a enviar al servidor
        val mainPending = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        val infoPending = PendingIntent.getActivity(
            //context, 1, Intent(context, InfoActivity::class.java), //To create a new screenif it is required
            context, 1, Intent(context, MainActivity::class.java), //To create a new screenif it is required
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val bigImage = BitmapFactory.decodeResource(context.resources, R.drawable.noti_imagen1)

        val noti = NotificationCompat.Builder(context, NotiConfig.CHANNEL_ID)
            .setSmallIcon(R.drawable.noti_stat_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(mainPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setLargeIcon(bigImage)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bigImage)
                    .bigLargeIcon(null as Bitmap?)
            )
            .addAction(R.drawable.ic_info, "Más info", infoPending)
            .build()

        safeNotify(id, noti)   // usa el wrapper seguro
    }

    // ⭐️ FUNCIÓN CENTRAL PARA MOSTRAR PUSH DESDE FCM (Módulo 2) ⭐️
    fun showFCMNotification(
        notificationId: String,
        title: String,
        body: String,
        priorityLevel: Int = NotificationCompat.PRIORITY_HIGH
    ) {

        // --- 1. INTENT DE APERTURA (OPENED) ---
        // Este Intent se activa cuando el usuario hace clic en el cuerpo de la notificación.
        val openIntent = Intent(context, MainActivity::class.java).apply {
            // ✅ INYECTAR DATOS DE REPORTE: MainActivity usará esto para llamar a FastAPI
            putExtra(NotificationConstants.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationConstants.EXTRA_EVENT_TYPE, NotificationConstants.EVENT_OPENED)
            // Aseguramos que el intent sea único para evitar conflictos
            action = notificationId
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        val mainPending = PendingIntent.getActivity(
            context,
            notificationId.hashCode(), // Código de solicitud único
            openIntent,
            flags
        )

        // --- 2. CONSTRUCCIÓN DE LA NOTIFICACIÓN ---
        val noti = NotificationCompat.Builder(context, NotiConfig.CHANNEL_ID)
            // Asumo que R.drawable.noti_stat_notification existe
            .setSmallIcon(R.drawable.noti_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(mainPending) // El clic en el cuerpo reporta OPENED
            .setAutoCancel(true)
            .setPriority(priorityLevel)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        safeNotify(notificationId.hashCode(), noti)
    }


    fun showAgrimetAlert(alert: lat.agrimet.agrimet.model.Alert,id: Int) {
        val priority = when (alert.severity) {
            lat.agrimet.agrimet.model.Alert.Severity.CRITICAL -> NotificationCompat.PRIORITY_MAX
            lat.agrimet.agrimet.model.Alert.Severity.WARNING -> NotificationCompat.PRIORITY_HIGH
            lat.agrimet.agrimet.model.Alert.Severity.INFO -> NotificationCompat.PRIORITY_DEFAULT
        }
        val mainPending = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        val infoPending = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        val bigImage = BitmapFactory.decodeResource(context.resources, R.drawable.noti_imagen1)
        val noti = NotificationCompat.Builder(context, NotiConfig.CHANNEL_ID)
            .setSmallIcon(R.drawable.noti_stat_notification)
            .setContentTitle(alert.title)
            .setContentText(alert.body)
            .setContentIntent(mainPending)
            .setAutoCancel(true)
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setLargeIcon(bigImage)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bigImage)
                    .setSummaryText(alert.body)
            )
            .addAction(R.drawable.ic_info, alert.actionText, infoPending)
            .build()
        safeNotify(alert.id.hashCode(), noti)
    }

    private fun safeNotify(id: Int, notification: Notification) {
        // 1) ¿notificaciones habilitadas para la app?
        if (!nm.areNotificationsEnabled()) {
            Log.w("NotiHelper", "Notificaciones deshabilitadas para la app")
            return
        }
        // 2) En 33+ verifica permiso POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w("NotiHelper", "Falta permiso POST_NOTIFICATIONS (API33+)")
                return
            }
        }
        // 3) Enviar
        nm.notify(id, notification)
    }


}