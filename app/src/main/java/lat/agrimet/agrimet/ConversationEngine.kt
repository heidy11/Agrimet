package lat.agrimet.agrimet

import kotlin.math.roundToInt
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

//Singleton que simula el backend de la conversación.
object ConversationEngine {

    data class MessageContent(val html: String, val speak: String)
    data class Option(val text: String, val next: String, val icon: String? = null, val context: String? = null)
    data class ConversationNode(
        val message: String? = null,
        val action: ((String?) -> MessageContent)? = null,
        val options: List<Option>? = null,
        val followUp: String? = null
    )

    private var conversationState = mutableMapOf<String, Any>()

    //Simulación de datos
    private fun getDummyWeatherData(type: String): Map<String, String> {
        return if (type == "now") {
            mapOf(
                "temp" to "%.1f°C".format(Random.nextDouble(15.0, 25.0)),
                "humidity" to "${Random.nextInt(50, 90)}%",
                "wind" to "%.1f km/h".format(Random.nextDouble(5.0, 20.0)),
                "rain" to "0 mm"
            )
        } else { // últimas 24 hr
            mapOf(
                "temp_max" to "%.1f°C".format(Random.nextDouble(18.0, 26.0)),
                "temp_min" to "%.1f°C".format(Random.nextDouble(8.0, 15.0)),
                "rain" to "%.1f mm".format(Random.nextDouble(0.0, 5.0)),
                "wind_avg" to "%.1f km/h".format(Random.nextDouble(5.0, 15.0))
            )
        }
    }

    private fun getDummyForecast(): List<Map<String, String>> {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("EEEE d", Locale("es", "ES"))

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = "Mañana, ${format.format(calendar.time)}"

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val dayAfter = format.format(calendar.time).replaceFirstChar { it.uppercase() }

        return listOf(
            mapOf(
                "day" to tomorrow,
                "rain" to "%.1f mm".format(Random.nextDouble(0.0, 10.0)),
                "max" to "%.1f°C".format(Random.nextDouble(18.0, 26.0)),
                "min" to "%.1f°C".format(Random.nextDouble(8.0, 15.0))
            ),
            mapOf(
                "day" to dayAfter,
                "rain" to "%.1f mm".format(Random.nextDouble(0.0, 10.0)),
                "max" to "%.1f°C".format(Random.nextDouble(19.0, 27.0)),
                "min" to "%.1f°C".format(Random.nextDouble(9.0, 16.0))
            )
        )
    }

    private fun calculateWaterLoss(stage: String?): Double {
        val kValues = mapOf("emergencia" to 0.45, "desarrollo" to 0.75, "llenado" to 1.15, "cosecha" to 0.85)
        val k = kValues[stage] ?: 1.0
        return Random.nextDouble(10.0, 25.0) * k
    }


    private val conversationFlow = mapOf(
        "start" to ConversationNode(
            message = "¡Hola! Soy AGRIbot, tu asistente climático agrícola. Tengo información importante para ti. ¿Quieres saber más?",
            options = listOf(
                Option("Sí, quiero saber más", "mainMenu", "👍"),
                Option("No, gracias", "end_thanks", "👋")
            )
        ),
        "mainMenu" to ConversationNode(
            message = "¿Sobre qué te gustaría recibir información hoy?",
            options = listOf(
                Option("Reporte del clima", "climaReport", "🌦️"),
                Option("Pronóstico", "pronostico", "📅"),
                Option("Impacto en cultivo", "askCrop", "🌱"),
                Option("Alertas de riesgo", "alertas", "⚠️")
            )
        ),
        "climaReport" to ConversationNode(
            message = "Perfecto. ¿Qué te gustaría conocer?",
            options = listOf(
                Option("Clima de ahora", "climaAhora", "🕒"),
                Option("Últimas 24 horas", "clima24h", "⏳")
            )
        ),
        "climaAhora" to ConversationNode(action = { getDummyWeatherData("now").let { MessageContent("Ahora mismo en tu zona:<br>🌡️ <b>Temperatura:</b> ${it["temp"]}<br>💧 <b>Humedad:</b> ${it["humidity"]}<br>💨 <b>Viento:</b> ${it["wind"]}<br>🌧️ <b>Lluvia:</b> ${it["rain"]}", "Ahora mismo la temperatura es de ${it["temp"]}, la humedad del ${it["humidity"]}, el viento de ${it["wind"]}, y no se registra lluvia.") } }, followUp = "askImpactFromWeather"),
        "clima24h" to ConversationNode(action = { getDummyWeatherData("last24h").let { MessageContent("En las últimas 24 horas:<br>🔺 <b>Temp. Máxima:</b> ${it["temp_max"]}<br>🔻 <b>Temp. Mínima:</b> ${it["temp_min"]}<br>🌧️ <b>Lluvia acumulada:</b> ${it["rain"]}<br>💨 <b>Viento promedio:</b> ${it["wind_avg"]}", "En las últimas 24 horas, la temperatura máxima fue de ${it["temp_max"]}, la mínima de ${it["temp_min"]}, la lluvia acumulada fue ${it["rain"]}, y el viento promedio de ${it["wind_avg"]}.") } }, followUp = "askImpactFromWeather"),
        "askImpactFromWeather" to ConversationNode(
            message = "¿Te gustaría saber cómo estos valores impactan a tu cultivo?",
            options = listOf(
                Option("Sí, me interesa", "askCrop", "👍"),
                Option("No, por ahora", "anythingElse", "👎")
            )
        ),
        "pronostico" to ConversationNode(
            action = {
                val forecast = getDummyForecast()
                var html = "Pronóstico para los próximos 2 días:<br><br>"
                var speak = "Aquí tienes el pronóstico. "
                forecast.forEach { day ->
                    html += "<b>${day["day"]}:</b><br>🌧️ Lluvia: ${day["rain"]}, 🔺 Max: ${day["max"]}, 🔻 Min: ${day["min"]}<br><br>"
                    speak += "${day["day"]}, se espera una lluvia de ${day["rain"]}, con una máxima de ${day["max"]} y una mínima de ${day["min"]}. "
                }
                MessageContent(html.trimEnd('<', 'b', 'r', '>', '<', 'b', 'r', '>'), speak)
            },
            followUp = "askImpactFromForecast"
        ),
        "askImpactFromForecast" to ConversationNode(
            message = "¿Te interesa saber el impacto del clima de los últimos días?",
            options = listOf(
                Option("Sí, por favor", "askCrop", "👍"),
                Option("No, gracias", "ask7DayForecast", "👎")
            )
        ),
        "ask7DayForecast" to ConversationNode(
            message = "Entendido. Puedes ver el pronóstico de 7 días en la app completa.",
            options = listOf(
                Option("Volver al menú", "mainMenu", "↩️"),
                Option("Terminar", "end_feedback", "❌")
            )
        ),
        "askCrop" to ConversationNode(
            message = "¿Sobre qué cultivo trabajamos hoy?",
            options = listOf(Option("Papa", "askStage", "🥔"))
        ),
        "askStage" to ConversationNode(
            message = "¿En qué etapa está tu cultivo de papa?",
            options = listOf(
                Option("Emergencia (1 mes)", "calculateImpact", null, "emergencia"),
                Option("Desarrollo (1-2 meses)", "calculateImpact", null, "desarrollo"),
                Option("Llenado (hasta 3 meses)", "calculateImpact", null, "llenado"),
                Option("Cosecha (3-4 meses)", "calculateImpact", null, "cosecha")
            )
        ),
        "calculateImpact" to ConversationNode(
            action = { stage ->
                val waterLoss = calculateWaterLoss(stage)
                conversationState["waterLoss"] = waterLoss
                val formattedLoss = "%.1f".format(waterLoss)
                MessageContent(
                    html = "Entendido. Tu cultivo perdió <b>$formattedLoss mm</b> de agua en los últimos 7 días.<br><br>Esto puede cambiar si llovió.",
                    speak = "Según el clima de los últimos 7 días, tu cultivo perdió $formattedLoss milímetros de agua. Este valor puede cambiar si llovió en la zona."
                )
            },
            followUp = "askAboutRain"
        ),
        "askAboutRain" to ConversationNode(
            message = "¿Quieres saber si llovió la última semana y cómo impactó?",
            options = listOf(
                Option("Sí, quiero saber", "rainImpact", "👍"),
                Option("No, gracias", "askAboutIrrigation", "👎")
            )
        ),
        "rainImpact" to ConversationNode(
            action = {
                val waterLoss = conversationState["waterLoss"] as? Double ?: 20.0
                val accumulatedRain = Random.nextDouble(0.0, 15.0)
                val percentage = ((accumulatedRain / waterLoss) * 100).roundToInt()
                val formattedRain = "%.1f".format(accumulatedRain)
                MessageContent(
                    html = "La lluvia aportó <b>$formattedRain mm</b>. Esto representa un <b>$percentage%</b> de la necesidad de riego.",
                    speak = "La lluvia aportó $formattedRain milímetros, esto representa un $percentage por ciento de la necesidad de riego."
                )
            },
            followUp = "askAboutIrrigation"
        ),
        "askAboutIrrigation" to ConversationNode(
            message = "Para finalizar, ¿aplicaste riego en la última semana?",
            options = listOf(
                Option("Sí, regué", "end_thanks_app", "✅"),
                Option("No, no regué", "anythingElse", "❌")
            )
        ),
        "alertas" to ConversationNode(
            message = "No hay alertas de riesgo para tu zona. Te avisaremos si se detecta algo.",
            options = listOf(Option("Volver al menú", "mainMenu", "↩️"))
        ),
        "anythingElse" to ConversationNode(
            message = "¿Deseas conocer algo más?",
            options = listOf(
                Option("Ver pronóstico", "pronostico", "📅"),
                Option("No, eso es todo", "end_feedback", "❌")
            )
        ),
        "end_feedback" to ConversationNode(
            message = "Antes de irme, ¿te pareció útil la información?",
            options = listOf(
                Option("Sí, fue útil", "end_final", "👍"),
                Option("No mucho", "end_final", "👎")
            )
        ),
        "end_thanks" to ConversationNode(message = "De acuerdo. ¡Que tengas un buen día! Si me necesitas, aquí estaré."),
        "end_thanks_app" to ConversationNode(
            message = "¡Excelente! Mantener un buen registro del riego es muy importante.",
            options = listOf(
                Option("Volver al menú", "mainMenu", "↩️"),
                Option("Terminar", "end_feedback", "❌")
            )
        ),
        "end_final" to ConversationNode(message = "¡Muchas gracias por tu tiempo! Recuerda que en la App principal tienes más información. ¡Hasta pronto!")
    )

    fun getNode(key: String): ConversationNode? {
        return conversationFlow[key]
    }
}
