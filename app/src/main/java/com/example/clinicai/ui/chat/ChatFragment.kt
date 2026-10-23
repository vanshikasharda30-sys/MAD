package com.example.clinicai.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import com.example.clinicai.R
import com.example.clinicai.databinding.FragmentChatBinding
import com.example.clinicai.ui.common.appViewModels
import com.example.clinicai.ui.common.collectFlow
import com.example.clinicai.ui.common.hideKeyboard
import com.example.clinicai.ui.common.openDoctor
import com.example.clinicai.ui.common.openFindDoctors
import com.example.clinicai.ui.common.show
import com.example.clinicai.ui.common.snack
import com.example.clinicai.ui.common.toMessage
import com.google.android.material.chip.Chip
import com.example.clinicai.config.AppConstants
import kotlinx.coroutines.flow.combine

/** "Ask ClinicAI": the website's receptionist chat, backed by `POST /api/chat`. */
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val clinicId by lazy { arguments?.getString("clinicId") ?: AppConstants.DEFAULT_CLINIC_ID }
    private val viewModel by appViewModels {
        ChatViewModel(it.chat, it.prefs, clinicId, greeting = getString(R.string.chat_greeting))
    }
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ChatAdapter(
            onDoctorClick = { openDoctor(it) },
            onBrowseClick = { openFindDoctors(it) },
        )
        binding.messageList.adapter = adapter

        listOf(R.string.chat_starter_1, R.string.chat_starter_2, R.string.chat_starter_3).forEach { res ->
            val chip = layoutInflater.inflate(R.layout.view_filter_chip, binding.starters, false) as Chip
            chip.text = getString(res)
            chip.isCheckable = false
            chip.setOnClickListener { send(chip.text.toString()) }
            binding.starters.addView(chip)
        }

        binding.sendButton.setOnClickListener { send(binding.messageInput.text?.toString().orEmpty()) }
        binding.messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send(binding.messageInput.text?.toString().orEmpty()); true
            } else false
        }

        collectFlow(combine(viewModel.messages, viewModel.loading) { m, l -> m to l }) { (messages, loading) ->
            val rows = messages.map { ChatRow.Message(it) } + if (loading) listOf(ChatRow.Typing) else emptyList()
            adapter.submitList(rows) { binding.messageList.scrollToPosition(rows.lastIndex) }
            binding.startersScroll.show(messages.size == 1)
            binding.sendButton.isEnabled = !loading
        }
        collectFlow(viewModel.errors) { error ->
            binding.root.snack(error.toMessage(requireContext()))
        }
    }

    private fun send(text: String) {
        if (viewModel.send(text)) {
            binding.messageInput.setText("")
            binding.root.hideKeyboard()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
