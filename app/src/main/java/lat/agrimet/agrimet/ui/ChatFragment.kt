package lat.agrimet.agrimet.ui

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import lat.agrimet.agrimet.R
import lat.agrimet.agrimet.adapter.ChatAdapter
import lat.agrimet.agrimet.databinding.FragmentChatBinding
import lat.agrimet.agrimet.model.ChatbotRequest
import lat.agrimet.agrimet.model.ChatNodeResponse
import lat.agrimet.agrimet.model.ChatOption
import lat.agrimet.agrimet.model.ChatMessage
import lat.agrimet.agrimet.model.Sender
import lat.agrimet.agrimet.network.HttpClient
import lat.agrimet.agrimet.network.ChatService

import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.Locale
import java.util.UUID

class ChatFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var tts: TextToSpeech
    private var isTtsInitialized = false
    private var currentlySpeakingId: String? = null

    // 🛑 CONFIGURACIÓN DE RED 🛑
    private val BASE_URL = "http://142.44.243.119:9000/api/v1".toHttpUrl()

    // ⭐️ CORRECCIÓN 1: La clave de API es necesaria para el constructor
    private val AGRIMET_API_KEY = "mi_clave_secreta_123456"

    // ✅ CORRECCIÓN 2: Pasando la clave al constructor del servicio
    private val chatService by lazy {
        ChatService(HttpClient.client, BASE_URL, AGRIMET_API_KEY)
    }

    private val sessionId = UUID.randomUUID().toString()
    private var isBotResponding = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tts = TextToSpeech(requireContext(), this)
        setupRecyclerView()


        if (savedInstanceState == null) {
            getChatResponse(nodeKey = "start", context = null)
        }
    }
    override fun onStart() {
        super.onStart()

        if (chatAdapter.itemCount == 0) {
            updateOptions(emptyList())
            getChatResponse(nodeKey = "start", context = null)
        }
    }


    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter { textToSpeak ->
            speak(textToSpeak)
        }
        binding.recyclerViewChat.apply {
            adapter = chatAdapter

            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
        }
    }


    private fun getChatResponse(nodeKey: String, context: String?) {
        if (isBotResponding) return
        isBotResponding = true

        val request = ChatbotRequest(
            nodeKey = nodeKey,
            context = context,
            sessionId = sessionId
        )


        lifecycleScope.launch {
            try {
                val result = chatService.getChatNode(request)

                result.onSuccess { response: ChatNodeResponse ->
                    addMessageToChat(ChatMessage(
                        text = response.messageContent.html,
                        sender = Sender.BOT,
                        speakableText = response.messageContent.speak
                    ))

                    updateOptions(response.options)

                    response.followUp?.let { followUpKey ->
                        getChatResponse(nodeKey = followUpKey, context = null)
                    }

                }.onFailure { error ->
                    Log.e("ChatFragment", "Error al obtener nodo de chat de la API", error)
                    showError("Error de conexión: Por favor, verifica la URL o el estado del servidor.")
                }
            } finally {
                isBotResponding = false
            }
        }
    }

    private fun updateOptions(options: List<ChatOption>) {
        binding.chipGroupOptions.removeAllViews()
        if (options.isEmpty()) {
            binding.optionsScrollView.visibility = View.GONE
            return
        }

        binding.optionsScrollView.visibility = View.VISIBLE
        options.forEach { option ->
            val chip = Chip(requireContext()).apply {
                text = if (option.icon != null) "${option.icon} ${option.text}" else option.text
                isClickable = true
                isCheckable = false
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                setOnClickListener {
                    onOptionSelected(option)
                }
            }
            binding.chipGroupOptions.addView(chip)
        }
    }


    private fun onOptionSelected(option: ChatOption) {
        if (tts.isSpeaking) {
            tts.stop()
        }

        val userMessageText = if (option.icon != null) "${option.icon} ${option.text}" else option.text
        addMessageToChat(ChatMessage(text = userMessageText, sender = Sender.USER))

        updateOptions(emptyList())

        getChatResponse(nodeKey = option.next, context = option.context)
    }



    private fun addMessageToChat(message: ChatMessage) {
        chatAdapter.addMessage(message)
        binding.recyclerViewChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }



    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("es", "ES"))

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "El idioma español no está soportado.")
            } else {
                isTtsInitialized = true
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        currentlySpeakingId = utteranceId
                    }
                    override fun onDone(utteranceId: String?) {
                        currentlySpeakingId = null
                    }
                    override fun onError(utteranceId: String?) {
                        currentlySpeakingId = null
                    }
                })
            }
        } else {
            Log.e("TTS", "Falló la inicialización de TextToSpeech.")
        }
    }


    private fun speak(text: String) {
        if (!::tts.isInitialized || !isTtsInitialized) {
            Log.e("TTS", "TextToSpeech no está inicializado.")
            return
        }

        if (tts.isSpeaking && currentlySpeakingId == text) {
            tts.stop()
            currentlySpeakingId = null
            return
        }

        val cleanText = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
        tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}