package lat.agrimet.agrimet.notificacion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import lat.agrimet.agrimet.MainActivity
import lat.agrimet.agrimet.R
import lat.agrimet.agrimet.model.Alert

/**
 * Constantes para evitar duplicidad de nombres en el proyecto.
 */
object NotificationConstants {
    const val CHANNEL_ID = "agrimet_fcm_channel"
    const val CHANNEL_NAME = "Notificaciones Agrimet"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_EVENT_TYPE = "extra_event_type"
    const val EVENT_OPENED = "OPENED"
}

class NotiHelper(private val context: Context) {

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                NotificationConstants.CHANNEL_ID,
                NotificationConstants.CHANNEL_NAME,
                importance
            ).apply {
                description = "Canal para alertas climáticas y notificaciones FCM"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Muestra una notificación recibida por FCM (Módulo 2).
     */
    fun showFCMNotification(notificationId: String, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(NotificationConstants.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationConstants.EXTRA_EVENT_TYPE, NotificationConstants.EVENT_OPENED)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notifySafe(notificationId.hashCode(), builder.build())
    }

    /**
     * ⭐️ FUNCIÓN REQUERIDA POR EL WORKER ⭐️
     * Muestra una notificación con estilo expandido.
     */
    fun sendBigPictureWithAction(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notifySafe(System.currentTimeMillis().toInt(), builder.build())
    }

    /**
     * Muestra una alerta climática simple (Módulo 1).
     */
    fun showAgrimetAlert(alert: Alert) {
        val builder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(alert.title)
            .setContentText(alert.body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notifySafe(alert.id.hashCode(), builder.build())
    }

    private fun notifySafe(id: Int, notification: android.app.Notification) {
        try {
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                NotificationManagerCompat.from(context).notify(id, notification)
            }
        } catch (e: SecurityException) {
            android.util.Log.e("NotiHelper", "Error de permisos de notificación", e)
        }
    }
}