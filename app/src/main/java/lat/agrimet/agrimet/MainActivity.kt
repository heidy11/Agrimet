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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import lat.agrimet.agrimet.databinding.ActivityMainBinding
import lat.agrimet.agrimet.model.WeatherPayload
import lat.agrimet.agrimet.model.notifications.NotificationReportRequest
import lat.agrimet.agrimet.model.notifications.TokenRegistrationRequest
import lat.agrimet.agrimet.network.HttpClient
import lat.agrimet.agrimet.network.NotificationManagementService
import lat.agrimet.agrimet.network.TcpClient
import lat.agrimet.agrimet.notificacion.NotificationConstants
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val tcpHost = "142.44.243.119"
    private val tcpPort = 9010

    private val client by lazy { TcpClient(tcpHost, tcpPort) }
    private val USER_LATITUDE = -16.5000
    private val USER_LONGITUDE = -68.1500

    // 🛑 FCM / REPORTES (MÓDULO 2) 🛑
    // URL Base para FastAPI (Usando el puerto 8000)
    private val BASE_URL = "http://142.44.243.119:8000/api/v1".toHttpUrl()

    private val notificationService by lazy {
        NotificationManagementService(HttpClient.client, BASE_URL)
    }

    // ID de Usuario Anónimo Persistente
    private var CURRENT_USER_ID: Long = 0L
    private val PREFS_NAME = "AgrimetUserPrefs"
    private val PREF_USER_ID = "anonymous_user_id"

    // Launcher para solicitar el permiso POST_NOTIFICATIONS (API 33+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.i("MainActivity", "Permiso de notificaciones concedido.")
        } else {
            Log.w("MainActivity", "Permiso de notificaciones denegado.")
        }
    }
    // 🛑 FIN FCM / REPORTES 🛑

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⭐️ NUEVO: OBTENER O CREAR ID ANÓNIMO ⭐️
        CURRENT_USER_ID = getOrCreateUserId()
        Log.i("MainActivity", "ID de usuario anónimo activo: $CURRENT_USER_ID")


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
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        //setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // ⭐️ INICIO MÓDULO 2: GESTIÓN DE FCM Y REPORTES ⭐️

        // 1. SOLICITAR PERMISO DE NOTIFICACIONES (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 2. INICIAR REGISTRO DE TOKEN (Se obtiene y se envía a FastAPI)
        initializeFCMRegistration()

        // 3. MANEJAR INTENT DE NOTIFICACIÓN (Si la actividad se lanzó por un click en Push)
        // El Intent en onCreate siempre es no nulo (Intent)
        handleNotificationIntent(intent)

        // ⭐️ FIN MÓDULO 2 ⭐️


        // ... (Tu código existente para TCP y UI) ...
        client.start()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                client.incoming.collect { line ->
                    Log.d("TCP", "Servidor dice: $line")
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // FUNCIONES DE GESTIÓN DE ID ANÓNIMO
    // ----------------------------------------------------------------------

    /**
     * Obtiene el ID de usuario persistente desde SharedPreferences.
     * Si no existe, genera un nuevo ID Long y lo guarda.
     * @return Long: ID único para esta instalación de la app.
     */
    private fun getOrCreateUserId(): Long {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var userId = prefs.getLong(PREF_USER_ID, 0L)

        if (userId == 0L) {
            // Genera un Long seguro basado en un UUID para asegurar unicidad
            userId = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
            prefs.edit().putLong(PREF_USER_ID, userId).apply()
            Log.i("MainActivity", "Nuevo ID anónimo generado: $userId")
        }
        return userId
    }

    // ----------------------------------------------------------------------
    // FUNCIONES DE FCM Y REPORTE DE EVENTOS (MÓDULO 2)
    // ----------------------------------------------------------------------

    /**
     * Obtiene el token de FCM y llama al servicio para registrarlo en FastAPI.
     * Utiliza el ID persistente creado en getOrCreateUserId().
     */
    private fun initializeFCMRegistration() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fallo al obtener el token de registro", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result

            // Llamar al servicio de red para registrar/actualizar el token en FastAPI
            lifecycleScope.launch {
                val request = TokenRegistrationRequest(
                    userId = CURRENT_USER_ID,
                    fcmToken = token,
                    platform = "Android"
                )

                val result = notificationService.registerToken(request)

                result.onSuccess {
                    Log.i("FCM", "Token registrado exitosamente en FastAPI para ID: $CURRENT_USER_ID")
                }.onFailure { error ->
                    Log.e("FCM", "Fallo al registrar token en FastAPI: ${error.message}", error)
                }
            }
        }
    }

    /**
     * Revisa si la actividad se lanzó debido a un click en una notificación (Intent).
     */
    private fun handleNotificationIntent(intent: Intent) {

        val notificationId = intent.getStringExtra(NotificationConstants.EXTRA_NOTIFICATION_ID)
        val eventType = intent.getStringExtra(NotificationConstants.EXTRA_EVENT_TYPE)

        // Solo procesamos si tenemos ambos datos Y el evento es 'OPENED'
        if (notificationId != null && eventType == NotificationConstants.EVENT_OPENED) {

            Log.d("FCM_Report", "Notificación abierta detectada. ID: $notificationId")

            // Limpiar el Intent para evitar que se reporte de nuevo en rotaciones/recreaciones
            intent.removeExtra(NotificationConstants.EXTRA_NOTIFICATION_ID)
            intent.removeExtra(NotificationConstants.EXTRA_EVENT_TYPE)

            reportNotificationEvent(notificationId, eventType)
        }
    }

    /**
     * Llama al servicio de red para informar a FastAPI sobre la interacción del usuario.
     */
    private fun reportNotificationEvent(notificationId: String, eventType: String) {
        lifecycleScope.launch {
            val request = NotificationReportRequest(
                userId = CURRENT_USER_ID,
                notificationId = notificationId,
                eventType = eventType,
                eventTimestamp = System.currentTimeMillis()
            )

            val result = notificationService.reportEvent(request)

            result.onSuccess {
                Log.i("FCM_Report", "Reporte de evento '$eventType' para ID $notificationId enviado exitosamente.")
            }.onFailure { error ->
                Log.e("FCM_Report", "Fallo al reportar evento '$eventType' para ID $notificationId: ${error.message}")
            }
        }
    }

    /**
     * Maneja el caso en que la Activity ya está abierta y recibe un nuevo Intent (ej. otro Push).
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // ⭐️ CORRECCIÓN: Manejar el Intent nulo del sistema y pasar uno no nulo a la función
        if (intent != null) {
            handleNotificationIntent(intent)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        client.stop()
    }
}