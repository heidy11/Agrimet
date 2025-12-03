package lat.agrimet.agrimet.network

import lat.agrimet.agrimet.model.IrrigationRequest
import lat.agrimet.agrimet.model.IrrigationResponse
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IrrigationService(private val client: OkHttpClient, private val baseUrl: HttpUrl,private val apiKey: String) {

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // --------------------------------------------------
    // 4. POST /api/v1/irrigation/calculate (Cálculo de Riego)
    // --------------------------------------------------
    suspend fun calculateIrrigation(request: IrrigationRequest): Result<IrrigationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    // Mapear el modelo de petición a JSON para el cuerpo (Request Body)
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
                        return@withContext Result.failure(
                            IOException("Irrigation API Error: HTTP ${response.code}")
                        )
                    }

                    val jsonString = response.body?.string() ?: "{}"
                    val json = JSONObject(jsonString)

                    val payload = IrrigationResponse(
                        waterLoss = json.getDouble("waterLoss"),
                        recommendation = json.getString("recommendation"),
                        riskLevel = json.getString("riskLevel")
                    )

                    Result.success(payload)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

}