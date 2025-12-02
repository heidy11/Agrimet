package lat.agrimet.agrimet.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import lat.agrimet.agrimet.R
import lat.agrimet.agrimet.adapter.AlertsAdapter
import lat.agrimet.agrimet.databinding.FragmentClimaBinding
import lat.agrimet.agrimet.model.Alert
import lat.agrimet.agrimet.model.WeatherPayload
import lat.agrimet.agrimet.network.HttpClient
import lat.agrimet.agrimet.network.WeatherService
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.Locale

class ClimaFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentClimaBinding? = null
    private val binding get() = _binding!!

    private lateinit var alertsAdapter: AlertsAdapter
    private var tts: TextToSpeech? = null
    private var currentlySpeakingId: String? = null

    // 🛑 ARQUITECTURA MODULAR: Inicialización del servicio WeatherService 🛑
    private val BASE_URL = "http://142.44.243.119:9000/api/v1".toHttpUrl()

    private val weatherService by lazy {
        //Toast.makeText(requireContext(), "Conectando Server Weather"+BASE_URL, Toast.LENGTH_SHORT).show()
        WeatherService(HttpClient.client, BASE_URL)
    }

    private val userLat = -16.5000
    private val userLon = -68.1500

    private var allAlerts: List<Alert> = emptyList()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClimaBinding.inflate(inflater, container, false)
        tts = TextToSpeech(requireContext(), this)

        //Toast.makeText(requireContext(), "Mensaje esta llegando", Toast.LENGTH_SHORT).show()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListeners()
        loadData() // Ahora llama a la API
    }

    private fun setupUI() {
        alertsAdapter = AlertsAdapter(emptyList()) { alerta ->
            speakOrStopAlert(alerta)
        }
        binding.alertsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = alertsAdapter
            isNestedScrollingEnabled = false
        }
        // Nota: Asumo que tienes otro adapter para el pronóstico (ForecastAdapter)
    }

    private fun setupListeners() {
        binding.buttonEscucharClima.setOnClickListener {
            speakOrStopWeatherData()
        }


        binding.buttonVerPronostico.setOnClickListener {
            val url = "https://eedes.tecnologia.bo/app/"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }


        binding.alertChipGroup.setOnCheckedChangeListener { _, checkedId ->
            // ✅ CORRECCIÓN 1: Convertir el ID del Chip directamente al Enum
            val severity: Alert.Severity? = when (checkedId) {
                R.id.chip_critical -> Alert.Severity.CRITICAL
                R.id.chip_warning -> Alert.Severity.WARNING
                R.id.chip_info -> Alert.Severity.INFO
                else -> null
            }
            filterAlerts(severity)
        }
    }

    private fun loadData() {
        // ⭐️ LÓGICA DE CONEXIÓN API ⭐️
        lifecycleScope.launch {

            //Toast.makeText(requireContext(), "Mensaje esta llegando", Toast.LENGTH_SHORT).show()

            // --- 1. Obtener Clima Actual (GET /weather/current) ---
            weatherService.getCurrentWeather(userLat, userLon)
                .onSuccess { payload ->
                    Log.d("ClimaFragment", "Clima Actual OK: ${payload.location}")
                    setWeather(payload) // Reemplaza la llamada mock
                }
                .onFailure { error ->
                    Log.e("ClimaFragment", "Error al obtener clima actual", error)
                    showError("Error al cargar clima: ${error.localizedMessage}")
                }

            // --- 2. Obtener Alertas (GET /alerts) ---
            weatherService.getAlerts(userLat, userLon)
                .onSuccess { alerts ->
                    Log.d("ClimaFragment", "Alertas OK: ${alerts.size} alertas")
                    allAlerts = alerts // Guarda la lista completa
                    filterAlerts(null) // Muestra todas las alertas inicialmente
                }
                .onFailure { error ->
                    Log.e("ClimaFragment", "Error al obtener alertas", error)
                    showError("No se pudieron cargar alertas.")
                }

            // --- 3. Obtener Pronóstico (GET /weather/forecast) ---
            weatherService.getForecast(userLat, userLon)
                .onSuccess { forecasts ->
                    Log.d("ClimaFragment", "Pronóstico OK: ${forecasts.size} días")
                    // forecastAdapter.updateData(forecasts) // Descomentar cuando el adaptador esté listo
                }
                .onFailure { error ->
                    Log.e("ClimaFragment", "Error al obtener pronóstico", error)
                }
        }
    }


    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "BO"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "El idioma español no está soportado en este dispositivo.")
            }
        } else {
            Log.e("TTS", "Falló la inicialización de TextToSpeech.")
        }
    }

    private fun speakOrStopWeatherData() {
        val weatherId = "weather_summary"
        if (tts?.isSpeaking == true && currentlySpeakingId == weatherId) {
            tts?.stop()
            currentlySpeakingId = null
            return
        }

        val textToSpeak = getWeatherDataAsString()
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, weatherId)
        currentlySpeakingId = weatherId
    }


    private fun speakOrStopAlert(alert: Alert) {
        if (tts?.isSpeaking == true && currentlySpeakingId == alert.id) {
            tts?.stop()
            currentlySpeakingId = null
            return
        }

        val textToSpeak = "Alerta. ${alert.title}. ${alert.body}"
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, alert.id)
        currentlySpeakingId = alert.id
    }

    //Construye y devuelve un string amigable con la información del clima.
    private fun getWeatherDataAsString(): String {
        return try {
            val temperature = binding.temperatureText.text.toString().replace("°C", "")
            val conditionAndLocation = binding.conditionText.text.toString().split(", ")
            val condition = if (conditionAndLocation.isNotEmpty()) conditionAndLocation[0] else "No disponible"
            val location = if (conditionAndLocation.size > 1) " en ${conditionAndLocation[1]}" else ""


            val humidity = binding.weatherMetrics.humidityValue.text.toString().replace("%", "")
            val precipitation = binding.weatherMetrics.rainValue.text.toString().replace(" mm", "")
            val wind = binding.weatherMetrics.windValue.text.toString().replace(" km/h", "")
            val uvIndex = binding.weatherMetrics.uvValue.text.toString()


            buildString {
                append("El pronóstico para hoy$location es: ")
                append("temperatura de $temperature grados Celsius, con cielo $condition. ")
                append("Además, se registra una humedad del $humidity por ciento, ")
                append("una precipitación de $precipitation milímetros, ")
                append("viento de $wind kilómetros por hora, ")
                append("y un índice UV de $uvIndex.")
            }
        } catch (e: Exception) {
            Log.e("TTS_SPEAK", "No se pudieron encontrar las vistas de métricas del clima. Verifica los IDs en 'view_weather_metrics.xml'. Usando texto básico.", e)

            val temperature = binding.temperatureText.text.toString().replace("°C", " grados Celsius")
            val conditionAndLocation = binding.conditionText.text.toString().split(", ")
            val condition = if (conditionAndLocation.isNotEmpty()) conditionAndLocation[0] else "No disponible"
            val location = if (conditionAndLocation.size > 1) " en ${conditionAndLocation[1]}" else ""
            "El pronóstico para hoy es: temperatura de $temperature, con cielo $condition$location."
        }
    }


    fun setWeather(data: WeatherPayload) {


        binding.temperatureText.text = "${data.temperatureC}°C"

        val loc = data.location?.let { ", $it" } ?: ""
        binding.conditionText.text = "${data.condition}$loc"

        binding.weatherMetrics.humidityValue.text = "${data.humidityPct}%"
        binding.weatherMetrics.rainValue.text     = "${data.precipitationMm} mm"
        binding.weatherMetrics.windValue.text     = "${data.windKmh} km/h"
        binding.weatherMetrics.uvValue.text       = data.uvIndex
    }

    private fun filterAlerts(severity: Alert.Severity?) {

        val filteredList = if (severity == null) {
            allAlerts
        } else {

            allAlerts.filter { it.severity == severity }
        }
        alertsAdapter.updateData(filteredList)
    }


    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }


    override fun onDestroyView() {

        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroyView()
        _binding = null
    }
}