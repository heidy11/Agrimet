package lat.agrimet.agrimet.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import lat.agrimet.agrimet.R
import lat.agrimet.agrimet.databinding.FragmentIrrigationBinding
import lat.agrimet.agrimet.databinding.ViewSelectableCardBinding

import lat.agrimet.agrimet.model.IrrigationRequest
import lat.agrimet.agrimet.model.IrrigationResponse
import lat.agrimet.agrimet.network.HttpClient
import lat.agrimet.agrimet.network.IrrigationService
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class IrrigationFragment : Fragment() {

    private var _binding: FragmentIrrigationBinding? = null
    private val binding get() = _binding!!


    private val BASE_URL = "http://142.44.243.119:9000/api/v1".toHttpUrl()


    private val irrigationService by lazy {
        IrrigationService(HttpClient.client, BASE_URL, "mi_clave_secreta_123456")
    }

    private var selectedCropCard: View? = null
    private var selectedStageCard: View? = null
    private var selectedDaysCard: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIrrigationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupStep1()
        setupStep2()
        setupStep3()
        resetForm()
    }

    private fun setupStep1() {
        val cropCards = listOf(binding.cardCropPotato, binding.cardCropCorn, binding.cardCropQuinoa)

        bindCard(binding.cardCropPotato, R.drawable.ic_compost, "Papa")
        bindCard(binding.cardCropCorn, R.drawable.ic_grain, "Maíz")
        bindCard(binding.cardCropQuinoa, R.drawable.ic_grass, "Quinua")

        cropCards.forEach { card ->
            card.root.setOnClickListener {
                handleSelection(it, cropCards) { selectedView ->
                    selectedCropCard = selectedView
                    binding.step2Title.visibility = View.VISIBLE
                    binding.stageFlow.visibility = View.VISIBLE
                    // Deseleccionar etapa y días para obligar a recalcular si cambia el cultivo
                    selectedStageCard = null
                    selectedDaysCard = null
                    binding.resultCard.visibility = View.GONE
                    scrollToView(binding.step2Title)
                }
            }
        }
    }

    private fun setupStep2() {
        val stageCards = listOf(binding.cardStageEmergency, binding.cardStageDev, binding.cardStageBloom)

        bindCard(binding.cardStageEmergency, R.drawable.ic_spoke, "Emergencia")
        bindCard(binding.cardStageDev, R.drawable.ic_eco, "Desarrollo")
        bindCard(binding.cardStageBloom, R.drawable.ic_local_florist, "Floración")

        stageCards.forEach { card ->
            card.root.setOnClickListener {
                handleSelection(it, stageCards) { selectedView ->
                    selectedStageCard = selectedView
                    binding.step3Title.visibility = View.VISIBLE
                    binding.daysFlow.visibility = View.VISIBLE

                    selectedDaysCard = null
                    binding.resultCard.visibility = View.GONE
                    scrollToView(binding.step3Title)
                }
            }
        }
    }

    private fun setupStep3() {
        val dayCards = listOf(binding.cardDays0, binding.cardDays1, binding.cardDays2, binding.cardDays3, binding.cardDays4, binding.cardDays5)

        bindCard(binding.cardDays0, null, "Hoy")
        bindCard(binding.cardDays1, null, "1 día")
        bindCard(binding.cardDays2, null, "2 días")
        bindCard(binding.cardDays3, null, "3 días")
        bindCard(binding.cardDays4, null, "4 días")
        bindCard(binding.cardDays5, null, "5+ días")

        dayCards.forEach { card ->
            card.root.setOnClickListener {
                handleSelection(it, dayCards) { selectedView ->
                    selectedDaysCard = selectedView
                    calculateAndShowResult() // Llama a la API
                    // No hacemos scroll aquí, lo hace la función calculateAndShowResult
                }
            }
        }
    }

    private fun bindCard(cardBinding: ViewSelectableCardBinding, iconRes: Int?, labelText: String) {
        val iconView = cardBinding.root.findViewById<ImageView>(R.id.option_icon)
        val labelView = cardBinding.root.findViewById<TextView>(R.id.option_label)

        if (iconRes != null) {
            iconView.setImageResource(iconRes)
            iconView.visibility = View.VISIBLE
        } else {
            iconView.visibility = View.GONE
        }
        labelView.text = labelText
    }

    private fun handleSelection(selectedView: View, group: List<ViewSelectableCardBinding>, onSelected: (View) -> Unit) {
        group.forEach { cardBinding ->
            cardBinding.root.isSelected = false
        }
        selectedView.isSelected = true
        onSelected(selectedView)
    }

    private fun deselectCardGroup(vararg cardBindings: ViewSelectableCardBinding) {
        cardBindings.forEach { it.root.isSelected = false }
    }

    private fun scrollToView(view: View) {
        (binding.root as? ScrollView)?.post {
            (binding.root as ScrollView).smoothScrollTo(0, view.top)
        }
    }

    private fun resetForm() {
        binding.step2Title.visibility = View.GONE
        binding.stageFlow.visibility = View.GONE
        binding.step3Title.visibility = View.GONE
        binding.daysFlow.visibility = View.GONE
        binding.resultCard.visibility = View.GONE



        selectedCropCard = null
        selectedStageCard = null
        selectedDaysCard = null
    }

    private fun calculateAndShowResult() {
        // --- 1. Mapear la UI a Strings de la API ---
        val cropType = when (selectedCropCard?.id) {
            R.id.card_crop_potato -> "POTATO"
            R.id.card_crop_corn -> "CORN"
            R.id.card_crop_quinoa -> "QUINOA"
            else -> null
        }

        val cropStage = when (selectedStageCard?.id) {
            R.id.card_stage_emergency -> "EMERGENCY"
            R.id.card_stage_dev -> "DEVELOPMENT"
            R.id.card_stage_bloom -> "BLOOMING"
            else -> null
        }

        val daysSinceWatered = when (selectedDaysCard?.id) {
            R.id.card_days_0 -> 0
            R.id.card_days_1 -> 1
            R.id.card_days_2 -> 2
            R.id.card_days_3 -> 3
            R.id.card_days_4 -> 4
            R.id.card_days_5 -> 5
            else -> null
        }

        if (cropType == null || cropStage == null || daysSinceWatered == null) {
            binding.resultCard.visibility = View.VISIBLE
            binding.resultMessage.text = getString(R.string.error_calculation)
            binding.resultDetail.text = getString(R.string.error_calculation_detail)
            scrollToView(binding.resultCard)
            return
        }

        // --- 2. Preparar UI para la llamada API ---
        binding.resultCard.visibility = View.VISIBLE
        binding.resultMessage.text = "Calculando..."
        binding.resultDetail.text = "Enviando datos al servidor para el cálculo..."

        val requestData = IrrigationRequest(cropType, cropStage, daysSinceWatered)


        lifecycleScope.launch {

            val result = irrigationService.calculateIrrigation(requestData)

            result.onSuccess { response: IrrigationResponse ->

                binding.resultMessage.text = getString(R.string.result_water_loss, response.waterLoss)
                binding.resultDetail.text = response.recommendation
                Log.d("Irrigation", "Cálculo exitoso desde API: ${response.waterLoss}mm")
                scrollToView(binding.resultCard)

            }.onFailure { error ->
                // Mostrar error de conexión
                binding.resultMessage.text = getString(R.string.error_calculation)
                binding.resultDetail.text = "Error de conexión con el backend: ${error.localizedMessage}. Inténtalo de nuevo."
                Log.e("Irrigation", "Fallo al calcular riego: ${error.localizedMessage}")
                scrollToView(binding.resultCard)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}