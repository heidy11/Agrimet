package lat.agrimet.agrimet.model

// Modelo de datos para la petición de POST /api/v1/irrigation/calculate
data class IrrigationRequest(
    val cropType: String,
    val cropStage: String,
    val daysSinceWatered: Int
)

// Modelo de datos para la respuesta de POST /api/v1/irrigation/calculate
data class IrrigationResponse(
    val waterLoss: Double,
    val recommendation: String,
    val riskLevel: String?
)
data class IrrigationResponseWrapper(
    val endpoint: String,
    val descripcion: String,
    val request: IrrigationRequest,
    val resultado: IrrigationResponse // El objeto anidado que contiene los datos reales
)