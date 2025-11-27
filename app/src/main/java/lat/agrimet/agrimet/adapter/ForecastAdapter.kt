package lat.agrimet.agrimet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import lat.agrimet.agrimet.R // Necesario para R.drawable
import lat.agrimet.agrimet.databinding.ItemPronosticoBinding
import lat.agrimet.agrimet.model.Forecast

class ForecastAdapter(private var forecasts: List<Forecast>) :
    RecyclerView.Adapter<ForecastAdapter.PronosticoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PronosticoViewHolder {
        val binding = ItemPronosticoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PronosticoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PronosticoViewHolder, position: Int) {
        holder.bind(forecasts[position])
    }

    override fun getItemCount(): Int = forecasts.size

    fun updateData(newForecasts: List<Forecast>) {
        forecasts = newForecasts
        notifyDataSetChanged()
    }

    class PronosticoViewHolder(private val binding: ItemPronosticoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // ⭐️ FUNCIÓN CLAVE: Mapea el nombre del icono (String) a un recurso local (Int)
        private fun getIconResourceId(iconName: String): Int {
            val context = binding.root.context

            val resourceId = context.resources.getIdentifier(
                iconName,
                "drawable",
                context.packageName
            )

            // ✅ CORRECCIÓN: Usamos un ícono de fallback común (ic_info) en lugar de ic_default_weather
            // Si ic_info tampoco existe, reemplázalo con un ID de recurso que sí tengas en R.drawable
            return if (resourceId != 0) resourceId else R.drawable.ic_info
        }

        fun bind(forecast: Forecast) {
            binding.forecastDate.text = forecast.day
            binding.forecastCondition.text = forecast.condition
            binding.forecastTemps.text = "${forecast.highTemp}° / ${forecast.lowTemp}°"

            val iconId = getIconResourceId(forecast.iconName)
            binding.forecastIcon.setImageResource(iconId)
        }
    }
}