package lat.agrimet.agrimet.network

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.runBlocking
import lat.agrimet.agrimet.network.pollNotification
import lat.agrimet.agrimet.notificacion.NotiHelper

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        return try {
            // Ejecutamos el suspend de pollNotification en bloque sincrónico
            val notif = runBlocking {
                pollNotification(
                    userId = "u123",
                    deviceId = "devA",
                    apiKey = "mi_clave_secreta_123456"
                )
            }

            if (notif != null) {
                // MISMA notificación que en MainActivity, usando NotiHelper
                NotiHelper(applicationContext).sendBigPictureWithAction(
                    title = "ALERTA MUNICIPAL",
                    message = "Incendios forestales: conoce las medidas de prevención."
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

}
