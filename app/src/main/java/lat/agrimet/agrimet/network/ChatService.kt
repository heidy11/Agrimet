package lat.agrimet.agrimet.network

import android.R
import lat.agrimet.agrimet.model.ChatbotRequest
import lat.agrimet.agrimet.model.ChatNodeResponse
import lat.agrimet.agrimet.model.MessageContent
import lat.agrimet.agrimet.model.ChatOption
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ChatService(
private val client: OkHttpClient,
private val baseUrl: HttpUrl,
private val apiKey: String
)
{

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // --------------------------------------------------
    // 5. POST /api/v1/conversation/node (Chatbot)
    // --------------------------------------------------
    suspend fun getChatNode(request: ChatbotRequest): Result<ChatNodeResponse> {
        return try {
            val jsonBody = JSONObject().apply {
                // Mapear los campos del modelo ChatbotRequest al cuerpo JSON
                put("nodeKey", request.nodeKey)
                put("context", request.context)
                put("sessionId", request.sessionId)
            }.toString().toRequestBody(JSON_MEDIA_TYPE)

            val url = baseUrl.newBuilder()
                .addPathSegment("conversation")
                .addPathSegment("node")
                .build()

            val req = Request.Builder()
                .url(url)
                .post(jsonBody)
                .header("X-API-key", apiKey)
                .build()

            client.newCall(req).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IOException("Chatbot API Error: HTTP ${response.code}"))
                }

                val json = JSONObject(response.body?.string() ?: "{}")

                // Deserialización de MessageContent
                val messageContentJson = json.getJSONObject("messageContent")
                val messageContent = MessageContent(
                    html = messageContentJson.getString("html"),
                    speak = messageContentJson.getString("speak")
                )

                // Deserialización del array de Opciones
                val optionsArray = json.getJSONArray("options")
                val options = mutableListOf<ChatOption>()
                for (i in 0 until optionsArray.length()) {
                    val opt = optionsArray.getJSONObject(i)
                    options.add(ChatOption(
                        text = opt.getString("text"),
                        next = opt.getString("next"),
                        // Usar optString para campos opcionales que pueden ser null
                        icon = opt.optString("icon", null),
                        context = opt.optString("context", null)
                    ))
                }

                // Construir el objeto de respuesta final
                val chatNodeResponse = ChatNodeResponse(
                    messageContent = messageContent,
                    options = options,
                    followUp = json.optString("followUp", null)
                )

                Result.success(chatNodeResponse)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}