package lat.agrimet.agrimet

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import lat.agrimet.agrimet.databinding.ActivityMainBinding
import lat.agrimet.agrimet.model.notifications.TokenRegistrationRequest
import lat.agrimet.agrimet.network.HttpClient
import lat.agrimet.agrimet.network.NotificationManagementService
import lat.agrimet.agrimet.notificacion.NotificationConstants
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Configuración del Backend para Notificaciones (Puerto 8000)
    private val BASE_URL = "http://142.44.243.119:8000/api/v1".toHttpUrl()

    private val notificationService by lazy {
        NotificationManagementService(HttpClient.client, BASE_URL)
    }

    private var CURRENT_USER_ID: Long = 0L
    private val PREFS_NAME = "AgrimetUserPrefs"
    private val PREF_USER_ID = "anonymous_user_id"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Inicialización de Firebase con el nuevo archivo google-services.json
        try {
            FirebaseApp.initializeApp(this)
            Log.i("FirebaseInit", "Firebase inicializado con éxito.")
        } catch (e: Exception) {
            Log.e("FirebaseInit", "Error al inicializar Firebase: ${e.message}")
        }

        super.onCreate(savedInstanceState)

        // Gestión de identidad anónima persistente
        CURRENT_USER_ID = getOrCreateUserId()

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
        )
        navView.setupWithNavController(navController)

        // Configuración de Notificaciones (Módulo 2)
        if (isFirebaseReady()) {
            setupNotificationPermissions()
            initializeFCMRegistration()
            handleNotificationIntent(intent)
        }
    }

    /**
     * Verifica que Firebase esté correctamente configurado antes de solicitar servicios.
     */
    private fun isFirebaseReady(): Boolean {
        return try {
            FirebaseApp.getInstance()
            true
        } catch (e: IllegalStateException) {
            Log.e("FCM", "Firebase no está listo. Verifica google-services.json y el plugin de Gradle.")
            false
        }
    }

    private fun setupNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()){}.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun initializeFCMRegistration() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fallo al obtener el token de Firebase", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM", "Token obtenido: $token")

            lifecycleScope.launch {
                try {
                    val request = TokenRegistrationRequest(CURRENT_USER_ID, token, "Android")
                    val result = notificationService.registerToken(request)

                    result.onSuccess {
                        Log.i("FCM", "Token registrado exitosamente en el Backend.")
                    }.onFailure { e ->
                        Log.e("FCM", "Error al registrar token en el Backend: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.e("Network", "Error de red: ${e.message}")
                }
            }
        }
    }

    private fun getOrCreateUserId(): Long {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var userId = prefs.getLong(PREF_USER_ID, 0L)
        if (userId == 0L) {
            userId = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
            prefs.edit().putLong(PREF_USER_ID, userId).apply()
        }
        return userId
    }

    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let {
            val notificationId = it.getStringExtra(NotificationConstants.EXTRA_NOTIFICATION_ID)
            val eventType = it.getStringExtra(NotificationConstants.EXTRA_EVENT_TYPE)
            if (notificationId != null && eventType == NotificationConstants.EVENT_OPENED) {
                it.removeExtra(NotificationConstants.EXTRA_NOTIFICATION_ID)
                it.removeExtra(NotificationConstants.EXTRA_EVENT_TYPE)
                reportNotificationEvent(notificationId, eventType)
            }
        }
    }

    private fun reportNotificationEvent(notificationId: String, eventType: String) {
        lifecycleScope.launch {
            try {
                val request = lat.agrimet.agrimet.model.notifications.NotificationReportRequest(
                    userId = CURRENT_USER_ID,
                    notificationId = notificationId,
                    eventType = eventType,
                    eventTimestamp = System.currentTimeMillis()
                )
                notificationService.reportEvent(request)
                Log.i("Analytics", "Evento $eventType reportado para notificación $notificationId")
            } catch (e: Exception) {
                Log.e("Network", "Error reportando evento al backend: ${e.message}")
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }
}