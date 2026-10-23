package com.example.clinicai.ui.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.auth.AuthState
import com.example.clinicai.data.auth.ClerkAuthManager
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.data.repository.AppointmentRepository
import com.example.clinicai.data.repository.ReceiptRepository
import com.example.clinicai.domain.AppointmentItem
import com.example.clinicai.domain.AppointmentItemMapper
import com.example.clinicai.ui.common.UiState
import com.example.clinicai.util.Validators
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppointmentsContent(
    val items: List<AppointmentItem>,
    /** The history API isn't live on this deployment; showing bookings kept on this device. */
    val localOnly: Boolean,
)

sealed interface AppointmentsEvent {
    data object Cancelled : AppointmentsEvent
    data object Reviewed : AppointmentsEvent
    data class Failed(val error: AppError) : AppointmentsEvent
    data class InvalidInput(val reason: Reason) : AppointmentsEvent
    enum class Reason { RATING }
}

class AppointmentsViewModel(
    private val appointments: AppointmentRepository,
    private val receipts: ReceiptRepository,
    private val auth: ClerkAuthManager,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<AppointmentsContent>>(UiState.Loading)
    val state: StateFlow<UiState<AppointmentsContent>> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AppointmentsEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<AppointmentsEvent> = _events.asSharedFlow()

    private val inFlight = mutableSetOf<String>()

    val isSignedIn: Boolean get() = auth.isSignedIn

    init {
        // Re-evaluate whenever the sign-in state changes (restored session, sign-in, sign-out).
        viewModelScope.launch {
            auth.authState.collect { if (it !is AuthState.Initializing) load() }
        }
    }

    fun load() {
        when (auth.authState.value) {
            AuthState.Initializing -> { _state.value = UiState.Loading; return }
            AuthState.SignedOut -> { _state.value = UiState.Error(AppError.Unauthorized()); return }
            is AuthState.SignedIn -> Unit
        }
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = appointments.list()) {
                is ApiResult.Success -> {
                    val items = result.data.map(AppointmentItemMapper::fromAppointment)
                    if (items.isEmpty()) UiState.Empty() else UiState.Success(AppointmentsContent(items, localOnly = false))
                }
                is ApiResult.Failure ->
                    if (result.error is AppError.EndpointMissing) {
                        val saved = receipts.allSaved().map(AppointmentItemMapper::fromReceipt)
                        if (saved.isEmpty()) UiState.Empty() else UiState.Success(AppointmentsContent(saved, localOnly = true))
                    } else {
                        UiState.Error(result.error)
                    }
            }
        }
    }

    fun cancel(id: String) {
        if (!inFlight.add(id)) return
        viewModelScope.launch {
            when (val result = appointments.cancel(id)) {
                is ApiResult.Success -> {
                    _events.emit(AppointmentsEvent.Cancelled)
                    load()
                }
                is ApiResult.Failure -> _events.emit(AppointmentsEvent.Failed(result.error))
            }
            inFlight.remove(id)
        }
    }

    fun review(id: String, rating: Int, text: String?) {
        if (!Validators.isValidRating(rating)) {
            _events.tryEmit(AppointmentsEvent.InvalidInput(AppointmentsEvent.Reason.RATING))
            return
        }
        if (!inFlight.add("review:$id")) return
        viewModelScope.launch {
            when (val result = appointments.review(id, rating, text)) {
                is ApiResult.Success -> {
                    _events.emit(AppointmentsEvent.Reviewed)
                    load()
                }
                is ApiResult.Failure -> _events.emit(AppointmentsEvent.Failed(result.error))
            }
            inFlight.remove("review:$id")
        }
    }
}
