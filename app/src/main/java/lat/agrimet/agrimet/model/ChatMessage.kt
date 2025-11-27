package lat.agrimet.agrimet.model

/**
 * @property text El contenido del mensaje a mostrar. Admite HTML simple.
 * @property sender Indica si el mensaje es del usuario o del bot.
 * @property speakableText El texto limpio que el motor Text-to-Speech leerá en voz alta.
 */
data class ChatbotRequest(
    val nodeKey: String,
    val context: String?,
    val sessionId: String
)

// 2. Modelo de la respuesta completa del backend (el nodo de conversación)
data class ChatNodeResponse(
    val messageContent: MessageContent,
    val options: List<ChatOption>,
    val followUp: String?
)

// Modelos anidados de la API
data class MessageContent(
    val html: String, // Texto para mostrar en el chat (puede contener formato)
    val speak: String // Texto para leer en voz alta
)

data class ChatOption(
    val text: String, // Texto de la opción visible en la UI
    val next: String, // Clave del siguiente nodo
    val icon: String?,
    val context: String?
)

// 3. TU MODELO ORIGINAL (el que usa tu frontend)
// Lo mantenemos, pero ahora la respuesta de la API será convertida a este formato
data class ChatMessage(
    val text: String,
    val sender: Sender,
    val speakableText: String? = text
)
enum class Sender {
    USER,
    BOT
}