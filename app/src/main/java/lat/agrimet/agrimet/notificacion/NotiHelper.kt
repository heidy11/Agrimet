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
 * Constantes globales para evitar errores de redelaración.
 * Si ya tienes un archivo de constantes, asegúrate de que estos valores no estén repetidos.
 */
object NotificationConstants {
    const val CHANNEL_ID = "agrimet_fcm_channel"
    const val CHANNEL_NAME = "Notificaciones Agrimet"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_EVENT_TYPE = "extra_event_type"
    const val EVENT_OPENED = "OPENED"
    const val REQUEST_CODE = 100
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
                description = "Canal para alertas climáticas y notificaciones de Agrimet"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Muestra una notificación recibida por FCM y configura el Intent para el reporte.
     */
    fun showFCMNotification(notificationId: String, title: String, body: String) {
        // Intent que apunta a MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Pasamos los datos para que MainActivity reporte el evento 'OPENED'
            putExtra(NotificationConstants.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationConstants.EXTRA_EVENT_TYPE, NotificationConstants.EVENT_OPENED)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId.hashCode(), // ID único para el PendingIntent
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Asegúrate de que este icono exista
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            with(NotificationManagerCompat.from(context)) {
                // Usamos el hash del ID de la notificación para que no se sobrepongan
                notify(notificationId.hashCode(), builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Muestra una alerta climática (Módulo 1).
     */
    fun showAgrimetAlert(alert: Alert) {
        val builder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(alert.title)
            .setContentText(alert.body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(alert.id.hashCode(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}