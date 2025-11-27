package lat.agrimet.agrimet.model

data class WeatherPayload(
    val temperatureC: String,    // "25"
    val condition: String,       // "Despejado"
    val location: String?,       // "La Paz" o null
    val humidityPct: String,     // "80"
    val precipitationMm: String, // "12"
    val windKmh: String,         // "15"
    val uvIndex: String          // "Moderado"
)