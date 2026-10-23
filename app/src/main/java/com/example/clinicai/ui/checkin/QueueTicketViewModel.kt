package com.example.clinicai.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.model.QueueStatus
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.repository.QueueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Where a patient's ticket stands, derived only from the live queue status the backend returns. */
data class TicketProgress(
    val nowServing: Int?,
    val peopleAhead: Int?,
    val estimatedWaitMinutes: Int?,
    val isYourTurn: Boolean,
) {
    companion object {
        fun from(status: QueueStatus, ticket: Int): TicketProgress {
            val mine = status.waiting.firstOrNull { it.queueNumber == ticket }
            return TicketProgress(
                nowServing = status.currentlyServing?.queueNumber,
                peopleAhead = mine?.let { (it.position - 1).coerceAtLeast(0) },
                estimatedWaitMinutes = mine?.estimatedWaitMinutes,
                isYourTurn = status.currentlyServing?.queueNumber == ticket,
            )
        }
    }
}

class QueueTicketViewModel(
    private val queue: QueueRepository,
    private val doctorId: String,
    private val ticket: Int,
) : ViewModel() {

    private val _progress = MutableStateFlow<TicketProgress?>(null)
    val progress: StateFlow<TicketProgress?> = _progress.asStateFlow()

    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    suspend fun refresh() {
        when (val result = queue.status(doctorId)) {
            is ApiResult.Success -> {
                _offline.value = false
                _progress.value = TicketProgress.from(result.data, ticket)
            }
            is ApiResult.Failure -> _offline.value = true
        }
    }

    fun refreshAsync() {
        viewModelScope.launch { refresh() }
    }
}
