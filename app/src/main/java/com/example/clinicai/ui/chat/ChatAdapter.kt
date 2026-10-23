package com.example.clinicai.ui.chat

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clinicai.R
import com.example.clinicai.data.model.ChatMessage
import com.example.clinicai.databinding.ItemChatAiBinding
import com.example.clinicai.databinding.ItemChatDoctorBinding
import com.example.clinicai.databinding.ItemChatTypingBinding
import com.example.clinicai.databinding.ItemChatUserBinding
import com.example.clinicai.ui.common.show
import com.example.clinicai.util.DateFormats

sealed interface ChatRow {
    data class Message(val message: ChatMessage) : ChatRow
    data object Typing : ChatRow
}

class ChatAdapter(
    private val onDoctorClick: (String) -> Unit,
    private val onBrowseClick: (String) -> Unit,
) : ListAdapter<ChatRow, RecyclerView.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int) = when (val row = getItem(position)) {
        ChatRow.Typing -> TYPE_TYPING
        is ChatRow.Message -> if (row.message.fromUser) TYPE_USER else TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserHolder(ItemChatUserBinding.inflate(inflater, parent, false))
            TYPE_TYPING -> object : RecyclerView.ViewHolder(ItemChatTypingBinding.inflate(inflater, parent, false).root) {}
            else -> AiHolder(ItemChatAiBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val row = getItem(position)
        if (row is ChatRow.Message) {
            when (holder) {
                is UserHolder -> holder.binding.messageText.text = row.message.text
                is AiHolder -> holder.bind(row.message)
            }
        }
    }

    private inner class AiHolder(val binding: ItemChatAiBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            val context = binding.root.context
            binding.messageText.text = if (message.isError) context.getString(R.string.chat_error) else message.text
            val emergency = message.isEmergency || message.isError
            binding.messageText.setTextColor(ContextCompat.getColor(context, if (emergency) R.color.red else R.color.ink))
            binding.messageText.setTypeface(null, if (message.isEmergency) Typeface.BOLD else Typeface.NORMAL)

            val specializationId = message.specializationId
            binding.suggestions.removeAllViews()
            binding.suggestions.show(specializationId != null)
            if (specializationId == null) return

            val inflater = LayoutInflater.from(context)
            message.suggestedDoctors.forEach { doctor ->
                val card = ItemChatDoctorBinding.inflate(inflater, binding.suggestions, false)
                card.doctorInitials.text = doctor.initials
                card.doctorName.text = doctor.name
                card.doctorMeta.text = buildString {
                    append(if (doctor.hasReviews) "★ %.1f".format(doctor.avgRating) else context.getString(R.string.doctor_new))
                    append("  ·  ₹").append(DateFormats.rupees(doctor.consultationFee))
                }
                card.root.setOnClickListener { onDoctorClick(doctor.id) }
                binding.suggestions.addView(card.root)
            }
            binding.browseButton.setText(if (message.suggestedDoctors.isEmpty()) R.string.chat_view_doctors else R.string.chat_browse_all)
            binding.browseButton.setOnClickListener { onBrowseClick(specializationId) }
            binding.browseButton.visibility = View.VISIBLE
        }
    }

    private class UserHolder(val binding: ItemChatUserBinding) : RecyclerView.ViewHolder(binding.root)

    private object Diff : DiffUtil.ItemCallback<ChatRow>() {
        override fun areItemsTheSame(a: ChatRow, b: ChatRow) =
            (a is ChatRow.Message && b is ChatRow.Message && a.message.id == b.message.id) || (a is ChatRow.Typing && b is ChatRow.Typing)
        override fun areContentsTheSame(a: ChatRow, b: ChatRow) = a == b
    }

    private companion object {
        const val TYPE_USER = 0
        const val TYPE_AI = 1
        const val TYPE_TYPING = 2
    }
}
