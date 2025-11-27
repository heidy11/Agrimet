package lat.agrimet.agrimet.model

// Data class to represent a single forecast entry.
data class Forecast(
    val day: String,
    val condition: String,
    val highTemp: Int,
    val lowTemp: Int,
    val iconName: String
)
