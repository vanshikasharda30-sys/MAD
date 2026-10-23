package com.example.clinicai.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.repository.AppointmentRepository
import com.example.clinicai.data.repository.ReceiptRepository
import com.example.clinicai.domain.ReceiptData
import com.example.clinicai.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads the receipt for one appointment. The server copy wins (it knows the latest status and
 * deposit state); the locally cached copy of a booking made on this device is the fallback.
 */
class ReceiptViewModel(
    private val appointments: AppointmentRepository,
    private val receipts: ReceiptRepository,
    private val appointmentId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ReceiptData>>(UiState.Loading)
    val state: StateFlow<UiState<ReceiptData>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            when (val result = appointments.get(appointmentId)) {
                is ApiResult.Success -> {
                    val patient = receipts.patientInfo()
                    _state.value = UiState.Success(receipts.recordConfirmed(result.data, result.data.doctor, patient))
                }
                is ApiResult.Failure -> {
                    val cached = receipts.saved(appointmentId)
                    _state.value = if (cached != null) UiState.Success(cached) else UiState.Error(result.error)
                }
            }
        }
    }
}
