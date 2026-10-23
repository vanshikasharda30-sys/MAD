package com.example.clinicai.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.data.model.QueueStatus
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.data.repository.QueueRepository
import com.example.clinicai.data.repository.StaffRepository
import com.example.clinicai.ui.common.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StaffQueueViewModel(
    private val staff: StaffRepository,
    private val queue: QueueRepository,
    private val clinicId: String,
) : ViewModel() {

    private val _doctors = MutableStateFlow<UiState<List<Doctor>>>(UiState.Loading)
    val doctors: StateFlow<UiState<List<Doctor>>> = _doctors.asStateFlow()

    private val _selectedDoctorId = MutableStateFlow<String?>(null)
    val selectedDoctorId: StateFlow<String?> = _selectedDoctorId.asStateFlow()

    private val _status = MutableStateFlow<QueueStatus?>(null)
    val status: StateFlow<QueueStatus?> = _status.asStateFlow()

    /** Queue entry id + action currently being sent; used to disable that button and ignore repeat taps. */
    private val _pendingAction = MutableStateFlow<String?>(null)
    val pendingAction: StateFlow<String?> = _pendingAction.asStateFlow()

    private val _errors = MutableSharedFlow<AppError>(extraBufferCapacity = 4)
    val errors: SharedFlow<AppError> = _errors.asSharedFlow()

    init {
        loadDoctors()
    }

    fun loadDoctors() {
        _doctors.value = UiState.Loading
        viewModelScope.launch {
            when (val result = staff.clinicDoctors(clinicId)) {
                is ApiResult.Success -> {
                    _doctors.value = if (result.data.isEmpty()) UiState.Empty() else UiState.Success(result.data)
                    if (_selectedDoctorId.value == null) _selectedDoctorId.value = result.data.firstOrNull()?.id
                    refresh()
                }
                is ApiResult.Failure -> _doctors.value = UiState.Error(result.error)
            }
        }
    }

    fun select(doctorId: String) {
        if (_selectedDoctorId.value == doctorId) return
        _selectedDoctorId.value = doctorId
        _status.value = null
        viewModelScope.launch { refresh() }
    }

    suspend fun refresh() {
        val doctorId = _selectedDoctorId.value ?: return
        when (val result = queue.status(doctorId)) {
            is ApiResult.Success -> if (doctorId == _selectedDoctorId.value) _status.value = result.data
            is ApiResult.Failure -> _errors.emit(result.error)
        }
    }

    fun perform(entryId: String, action: QueueRepository.QueueAction) {
        val key = entryId + action.path
        if (_pendingAction.value != null) return
        _pendingAction.value = key
        viewModelScope.launch {
            when (val result = queue.perform(entryId, action)) {
                is ApiResult.Success -> refresh()
                is ApiResult.Failure -> _errors.emit(result.error)
            }
            _pendingAction.value = null
        }
    }
}
