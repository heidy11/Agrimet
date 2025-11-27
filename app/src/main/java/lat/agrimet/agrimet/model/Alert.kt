package lat.agrimet.agrimet.model

data class Alert(
    val id: String,
    val title: String,
    val timestamp: String,
    val body: String,
    val severity: Severity,
    val actionText: String
) {

    enum class Severity {
        CRITICAL, WARNING, INFO
    }
}
