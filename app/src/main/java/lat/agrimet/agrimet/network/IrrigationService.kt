package lat.agrimet.agrimet.network

import lat.agrimet.agrimet.model.IrrigationRequest
import lat.agrimet.agrimet.model.IrrigationResponse
import lat.agrimet.agrimet.model.IrrigationResponseWrapper // ⭐️ Necesario para la respuesta anidada
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Servicio dedicado al cálculo de riego (POST /irrigation/calculate).
 * Implementa la autenticación con la clave de API (X-API-Key).
 */
class IrrigationService(
    private val client: OkHttpClient,
    private val baseUrl: HttpUrl,
    private val apiKey: String
) {

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()


    suspend fun calculateIrrigation(request: IrrigationRequest): Result<IrrigationResponse> {
        return try {
            val jsonBody = JSONObject().apply {
                put("cropType", request.cropType)
                put("cropStage", request.cropStage)
                put("daysSinceWatered", request.daysSinceWatered)
            }.toString().toRequestBody(JSON_MEDIA_TYPE)

            val url = baseUrl.newBuilder()
                .addPathSegment("irrigation")
                .addPathSegment("calculate")
                .build()

            val req = Request.Builder()
                .url(url)
                .post(jsonBody)
                .header("X-API-key", apiKey)
                .build()

            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Irrigation API Error: HTTP ${response.code}"))
                }

                val jsonString = response.body?.string() ?: "{}"
                val json = JSONObject(jsonString)

                val resultadoJson = json.getJSONObject("resultado")

                val finalResponse = IrrigationResponse(

                    waterLoss = resultadoJson.getDouble("waterLoss"),

                    recommendation = resultadoJson.getString("recommendation"),

                    riskLevel = resultadoJson.optString("riskLevel", null)
                )


                Result.success(finalResponse)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}