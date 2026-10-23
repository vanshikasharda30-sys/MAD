package com.example.clinicai.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.local.SessionPreferences
import com.example.clinicai.data.model.ChatMessage
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chat: ChatRepository,
    private val prefs: SessionPreferences,
    private val clinicId: String,
    greeting: String,
) : ViewModel() {

    private var nextId = 1L

    private val _messages = MutableStateFlow(listOf(ChatMessage(id = 0, fromUser = false, text = greeting)))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errors = MutableSharedFlow<AppError>(extraBufferCapacity = 4)
    val errors: SharedFlow<AppError> = _errors.asSharedFlow()

    /** Sends [text] unless blank or a reply is still pending (prevents duplicate requests). */
    fun send(text: String): Boolean {
        val message = text.trim()
        if (message.isEmpty() || _loading.value) return false
        _loading.value = true
        _messages.update { it + ChatMessage(id = nextId++, fromUser = true, text = message) }

        viewModelScope.launch {
            when (val result = chat.send(clinicId, message)) {
                is ApiResult.Success -> {
                    val reply = result.data
                    val specializationId = reply.specializationId
                    if (reply.type == "symptom_match") prefs.saveSymptomNotes(message)
                    val doctors = if (specializationId != null) chat.suggestedDoctors(specializationId, clinicId) else emptyList()
                    _messages.update {
                        it + ChatMessage(
                            id = nextId++,
                            fromUser = false,
                            text = reply.reply,
                            type = reply.type,
                            specializationId = specializationId,
                            suggestedDoctors = doctors,
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _messages.update { it + ChatMessage(id = nextId++, fromUser = false, text = "", isError = true) }
                    _errors.emit(result.error)
                }
            }
            _loading.value = false
        }
        return true
    }
}
