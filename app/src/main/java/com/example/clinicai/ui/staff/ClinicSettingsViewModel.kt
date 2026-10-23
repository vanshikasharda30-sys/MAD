package com.example.clinicai.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.model.Clinic
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.data.repository.ClinicRepository
import com.example.clinicai.ui.common.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SettingsEvent {
    data object Saved : SettingsEvent
    data class Failed(val error: AppError) : SettingsEvent
}

class ClinicSettingsViewModel(private val clinics: ClinicRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<Clinic>>(UiState.Loading)
    val state: StateFlow<UiState<Clinic>> = _state.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = clinics.myClinic()) {
                is ApiResult.Success -> UiState.Success(result.data)
                is ApiResult.Failure -> UiState.Error(result.error)
            }
        }
    }

    fun save(phone: String, email: String) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            when (val result = clinics.updateMyClinic(phone, email)) {
                is ApiResult.Success -> { _state.value = UiState.Success(result.data); _events.emit(SettingsEvent.Saved) }
                is ApiResult.Failure -> _events.emit(SettingsEvent.Failed(result.error))
            }
            _saving.value = false
        }
    }
}
