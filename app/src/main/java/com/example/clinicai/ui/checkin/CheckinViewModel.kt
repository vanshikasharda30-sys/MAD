package com.example.clinicai.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.auth.ClerkAuthManager
import com.example.clinicai.data.model.Clinic
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.data.repository.ClinicRepository
import com.example.clinicai.data.repository.QueueRepository
import com.example.clinicai.ui.common.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CheckinEvent {
    data class Joined(val doctor: Doctor, val queueNumber: Int) : CheckinEvent
    data class JoinFailed(val error: AppError) : CheckinEvent
}

class CheckinViewModel(
    private val clinics: ClinicRepository,
    private val queue: QueueRepository,
    private val auth: ClerkAuthManager,
) : ViewModel() {

    /** Idle until a code is looked up; Loading / Success(clinic) / Error afterwards. */
    private val _clinic = MutableStateFlow<UiState<Clinic>>(UiState.Empty())
    val clinic: StateFlow<UiState<Clinic>> = _clinic.asStateFlow()

    private val _joiningDoctorId = MutableStateFlow<String?>(null)
    val joiningDoctorId: StateFlow<String?> = _joiningDoctorId.asStateFlow()

    private val _events = MutableSharedFlow<CheckinEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<CheckinEvent> = _events.asSharedFlow()

    private var lastToken: String? = null

    val isSignedIn: Boolean get() = auth.isSignedIn

    fun lookup(token: String) {
        lastToken = token
        _clinic.value = UiState.Loading
        viewModelScope.launch {
            _clinic.value = when (val result = clinics.resolveQr(token)) {
                is ApiResult.Success -> UiState.Success(result.data)
                is ApiResult.Failure -> UiState.Error(result.error)
            }
        }
    }

    fun retry() {
        lastToken?.let(::lookup)
    }

    /** Joins the live queue for [doctor]. Ignored while another join is in flight. */
    fun join(doctor: Doctor) {
        if (_joiningDoctorId.value != null) return
        _joiningDoctorId.value = doctor.id
        viewModelScope.launch {
            when (val result = queue.join(doctor.id)) {
                is ApiResult.Success -> _events.emit(CheckinEvent.Joined(doctor, result.data.queueNumber))
                is ApiResult.Failure -> _events.emit(CheckinEvent.JoinFailed(result.error))
            }
            _joiningDoctorId.value = null
        }
    }
}
