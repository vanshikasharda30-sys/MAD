package com.example.clinicai.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.model.StaffMember
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.data.repository.StaffRepository
import com.example.clinicai.ui.common.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MembersEvent {
    data object Invited : MembersEvent
    data class Failed(val error: AppError) : MembersEvent
}

class StaffMembersViewModel(private val staff: StaffRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<StaffMember>>>(UiState.Loading)
    val state: StateFlow<UiState<List<StaffMember>>> = _state.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _events = MutableSharedFlow<MembersEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<MembersEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = staff.members()) {
                is ApiResult.Success -> if (result.data.isEmpty()) UiState.Empty() else UiState.Success(result.data)
                is ApiResult.Failure -> UiState.Error(result.error)
            }
        }
    }

    fun invite(name: String, email: String, role: String) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            when (val result = staff.invite(name, email, role)) {
                is ApiResult.Success -> { _events.emit(MembersEvent.Invited); load() }
                is ApiResult.Failure -> _events.emit(MembersEvent.Failed(result.error))
            }
            _busy.value = false
        }
    }
}
