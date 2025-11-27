package lat.agrimet.agrimet.notificacion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotiConfig {
    const val CHANNEL_ID = "alerts_channel"
    const val CHANNEL_NAME = "Alertas"
    const val CHANNEL_DESC = "Notificaciones generales y de emergencia"
}

fun Context.createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            NotiConfig.CHANNEL_ID,
            NotiConfig.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH // heads-up
        ).apply { description = NotiConfig.CHANNEL_DESC }

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
}
