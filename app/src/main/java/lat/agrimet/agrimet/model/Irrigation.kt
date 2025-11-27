package lat.agrimet.agrimet.model

// Modelo de datos para la petición de POST /api/v1/irrigation/calculate
data class IrrigationRequest(
    val cropType: String,
    val cropStage: String,
    val daysSinceWatered: Int
)

// Modelo de datos para la respuesta de POST /api/v1/irrigation/calculate
data class IrrigationResponse(
    val waterLoss: Int,
    val recommendation: String
)