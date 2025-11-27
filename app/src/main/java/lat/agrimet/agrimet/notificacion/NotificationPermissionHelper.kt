package lat.agrimet.agrimet.notificacion

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Maneja: permiso POST_NOTIFICATIONS (API 33+), check de notificaciones habilitadas,
 * y atajos a Ajustes. Úsalo desde Activities y evita duplicar código.
 */
class NotificationPermissionHelper(
    private val activity: ComponentActivity,
    private val onGranted: () -> Unit,
    private val onDenied: () -> Unit = {}
) {
    private val launcher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) onGranted() else onDenied()
        }

    /** Llama esto para intentar enviar una notificación:
     *  - Verifica si están habilitadas a nivel del sistema
     *  - Pide POST_NOTIFICATIONS en 33+
     *  - Si todo ok → ejecuta onGranted()
     */
    fun ensurePermissionAndRun() {
        // A) ¿notificaciones habilitadas para la app?
        if (!NotificationManagerCompat.from(activity).areNotificationsEnabled()) {
            openAppNotificationSettings(activity)
            return
        }

        // B) En Android 13+ pide POST_NOTIFICATIONS si hace falta
        if (Build.VERSION.SDK_INT >= 33) {
            val has = ContextCompat.checkSelfPermission(
                activity, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!has) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        // C) Listo
        onGranted()
    }

    /** ¿El usuario marcó “No volver a preguntar”? (solo 33+) */
    fun shouldShowRationale(): Boolean {
        return Build.VERSION.SDK_INT >= 33 &&
                activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        /** Abre Ajustes > Notificaciones de la app */
        fun openAppNotificationSettings(context: Context) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        }

        /** Abre Ajustes > Info de la app (para dar permisos manualmente) */
        fun openAppDetailsSettings(context: Context) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }
}
