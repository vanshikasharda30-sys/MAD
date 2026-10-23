package com.example.clinicai.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.model.CreateDoctorRequest
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.data.model.Specialization
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.data.repository.DoctorRepository
import com.example.clinicai.data.repository.StaffRepository
import com.example.clinicai.ui.common.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DoctorsEvent {
    data object Added : DoctorsEvent
    data object Deactivated : DoctorsEvent
    data class Failed(val error: AppError) : DoctorsEvent
}

class StaffDoctorsViewModel(
    private val staff: StaffRepository,
    private val doctors: DoctorRepository,
    private val clinicId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Doctor>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Doctor>>> = _state.asStateFlow()

    private val _specializations = MutableStateFlow<List<Specialization>>(emptyList())
    val specializations: StateFlow<List<Specialization>> = _specializations.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _events = MutableSharedFlow<DoctorsEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<DoctorsEvent> = _events.asSharedFlow()

    init {
        load()
        viewModelScope.launch {
            (doctors.specializations() as? ApiResult.Success)?.let { _specializations.value = it.data }
        }
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = staff.clinicDoctors(clinicId)) {
                is ApiResult.Success -> if (result.data.isEmpty()) UiState.Empty() else UiState.Success(result.data)
                is ApiResult.Failure -> UiState.Error(result.error)
            }
        }
    }

    fun add(request: CreateDoctorRequest) = guarded {
        when (val result = staff.createDoctor(request)) {
            is ApiResult.Success -> { _events.emit(DoctorsEvent.Added); load() }
            is ApiResult.Failure -> _events.emit(DoctorsEvent.Failed(result.error))
        }
    }

    fun deactivate(doctorId: String) = guarded {
        when (val result = staff.deactivateDoctor(doctorId)) {
            is ApiResult.Success -> { _events.emit(DoctorsEvent.Deactivated); load() }
            is ApiResult.Failure -> _events.emit(DoctorsEvent.Failed(result.error))
        }
    }

    private fun guarded(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try { block() } finally { _busy.value = false }
        }
    }
}
