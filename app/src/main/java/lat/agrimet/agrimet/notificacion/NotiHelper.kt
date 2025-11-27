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

import lat.agrimet.agrimet.model.Alert

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



}