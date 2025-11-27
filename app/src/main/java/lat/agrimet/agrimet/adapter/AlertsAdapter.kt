package lat.agrimet.agrimet.adapter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import lat.agrimet.agrimet.R
import lat.agrimet.agrimet.databinding.ItemAlertaBinding
import lat.agrimet.agrimet.model.Alert
import com.google.android.material.R as RMaterial

class AlertsAdapter(
    private var alerts: List<Alert>,
    private val onItemClicked: (Alert) -> Unit
) : RecyclerView.Adapter<AlertsAdapter.AlertaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertaViewHolder {
        val binding = ItemAlertaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlertaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertaViewHolder, position: Int) {
        val alerta = alerts[position]
        holder.bind(alerta)

        holder.itemView.setOnClickListener {
            onItemClicked(alerta)
        }
    }

    override fun getItemCount(): Int = alerts.size

    fun updateData(newAlerts: List<Alert>) {
        alerts = newAlerts
        notifyDataSetChanged()
    }

    class AlertaViewHolder(private val binding: ItemAlertaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(alert: Alert) {
            binding.alertTitle.text = alert.title
            binding.alertTime.text = alert.timestamp
            binding.alertBody.text = alert.body
            binding.alertActionButton.text = alert.actionText

            val context = binding.root.context
            val iconRes: Int
            val iconColor: Int


            when (alert.severity) {
                Alert.Severity.CRITICAL -> {
                    iconRes = R.drawable.ic_error
                    iconColor = ContextCompat.getColor(context, RMaterial.color.design_default_color_error)
                }
                Alert.Severity.WARNING -> {
                    iconRes = R.drawable.ic_warning
                    iconColor = ContextCompat.getColor(context, R.color.md_theme_tertiary) // Example color
                }
                Alert.Severity.INFO -> {
                    iconRes = R.drawable.ic_info
                    iconColor = ContextCompat.getColor(context, R.color.md_theme_secondary) // Example color
                }
            }
            binding.alertIcon.setImageResource(iconRes)
            binding.alertIcon.setColorFilter(iconColor)
        }
    }
}
