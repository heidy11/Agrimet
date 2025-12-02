package lat.agrimet.agrimet

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import lat.agrimet.agrimet.databinding.ActivityMainBinding

//New for TCP
import lat.agrimet.agrimet.network.TcpClient
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

//For widgets
import androidx.navigation.fragment.NavHostFragment
import lat.agrimet.agrimet.ui.ClimaFragment
import lat.agrimet.agrimet.model.WeatherPayload

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val tcpHost = "142.44.243.119"   // Ip address from the server
    private val tcpPort = 9010

    // Usa el scope por defecto del TcpClient (o podrías pasar lifecycleScope)
    private val client by lazy { TcpClient(tcpHost, tcpPort) }
    private val USER_LATITUDE = -16.5000
    private val USER_LONGITUDE = -68.1500

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        //<<---------------------------------------->>
        //FUNCIONES PARA MANEJAR LOS ELEMENTOS DEL TEXTVIEW
        /*
        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment

        val climaFragment = navHost.childFragmentManager.fragments
            .firstOrNull { it is ClimaFragment } as? ClimaFragment

        climaFragment?.setWeather(
            WeatherPayload(
                temperatureC = "27",
                condition = "Parcialmente nublado",
                location = "La Paz",
                humidityPct = "95",
                precipitationMm = "5",
                windKmh = "12",
                uvIndex = "Alto"
            )
        )*/

        //<<---------------------------------------->>

        // ---- TCP: arrancar conexión y recolectar mensajes ----
        //<<---------------------------------------->>
        /*client.start()
        // Recolecta mensajes del servidor cuando la Activity esté al menos en STARTED
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                client.incoming.collect { line ->
                    Log.d("TCP", "Servidor dice: $line")
                    // Si quieres mostrar en UI:
                    // binding.miTextView.text = line
                }
            }
        }*/

        //Para disparar la notificacion
        //<<---------------------------------------->>
        // Crear canal de notificaciones (API 26+)
        client.start()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                client.incoming.collect { line ->
                    Log.d("TCP", "Servidor dice: $line")
                }
            }
        }
        }

    //Adding the onDestroy method to close the connection and cancel resources:
    override fun onDestroy() {
        super.onDestroy()
        // Cierra la conexión y cancela recursos
        client.stop()
    }
}