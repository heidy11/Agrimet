package lat.agrimet.agrimet.adapter

import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import lat.agrimet.agrimet.databinding.ItemChatMessageBotBinding
import lat.agrimet.agrimet.databinding.ItemChatMessageUserBinding
import lat.agrimet.agrimet.model.ChatMessage
import lat.agrimet.agrimet.model.Sender

/**
 * Adaptador para el RecyclerView que muestra la lista de mensajes del chat.
 * Maneja dos tipos de vistas: una para los mensajes del BOT y otra para los del USUARIO,
 * utilizando ViewBinding para acceder a las vistas de forma segura.
 *
 * @param onSpeakClicked Lambda que se invoca cuando el usuario presiona el botón de "escuchar".
 */
class ChatAdapter(
    private val onSpeakClicked: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    private companion object {
        const val VIEW_TYPE_USER = 1
        const val VIEW_TYPE_BOT = 2
    }

    // ViewHolder para los mensajes del usuario, con ViewBinding.
    inner class UserViewHolder(private val binding: ItemChatMessageUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.textViewUserMessage.text = message.text
        }
    }

    // ViewHolder para los mensajes del bot, con ViewBinding.
    inner class BotViewHolder(private val binding: ItemChatMessageBotBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.textViewBotMessage.text = Html.fromHtml(message.text, Html.FROM_HTML_MODE_COMPACT)
            binding.textViewBotMessage.movementMethod = LinkMovementMethod.getInstance()

            binding.buttonSpeak.setOnClickListener {
                message.speakableText?.let { textToSpeak ->
                    onSpeakClicked(textToSpeak)
                }
            }
        }
    }

    /**
     * Añade un nuevo mensaje a la lista y notifica al adaptador para que actualice la UI.
     */
    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].sender == Sender.USER) VIEW_TYPE_USER else VIEW_TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            val binding = ItemChatMessageUserBinding.inflate(inflater, parent, false)
            UserViewHolder(binding)
        } else { // VIEW_TYPE_BOT
            val binding = ItemChatMessageBotBinding.inflate(inflater, parent, false)
            BotViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder.itemViewType == VIEW_TYPE_USER) {
            (holder as UserViewHolder).bind(message)
        } else {
            (holder as BotViewHolder).bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size
}

