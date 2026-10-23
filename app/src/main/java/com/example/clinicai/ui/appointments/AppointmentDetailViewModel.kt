package com.example.clinicai.ui.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.model.Appointment
import com.example.clinicai.data.model.PaymentOrder
import com.example.clinicai.data.model.VerifyPaymentRequest
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.data.remote.PaymentResult
import com.example.clinicai.data.remote.PaymentResultBus
import com.example.clinicai.data.repository.AppointmentRepository
import com.example.clinicai.data.repository.PaymentRepository
import com.example.clinicai.data.repository.ReceiptRepository
import com.example.clinicai.domain.ReceiptData
import com.example.clinicai.ui.common.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** [appointment] is null when only a locally cached receipt is available (read-only). */
data class AppointmentDetail(val appointment: Appointment?, val receipt: ReceiptData)

sealed interface DetailEvent {
    data object Cancelled : DetailEvent
    data object Reviewed : DetailEvent
    data object DepositPaid : DetailEvent
    data class OpenCheckout(val order: PaymentOrder) : DetailEvent
    data class Failed(val error: AppError) : DetailEvent
    data class PaymentFailed(val description: String?) : DetailEvent
    data object RatingMissing : DetailEvent
}

class AppointmentDetailViewModel(
    private val appointments: AppointmentRepository,
    private val payments: PaymentRepository,
    private val receipts: ReceiptRepository,
    paymentBus: PaymentResultBus,
    private val appointmentId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<AppointmentDetail>>(UiState.Loading)
    val state: StateFlow<UiState<AppointmentDetail>> = _state.asStateFlow()

    private val _events = MutableSharedFlow<DetailEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<DetailEvent> = _events.asSharedFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            paymentBus.results.collect { result -> onPaymentResult(result) }
        }
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            when (val result = appointments.get(appointmentId)) {
                is ApiResult.Success -> {
                    val receipt = receipts.recordConfirmed(result.data, result.data.doctor, receipts.patientInfo())
                    _state.value = UiState.Success(AppointmentDetail(result.data, receipt))
                }
                is ApiResult.Failure -> {
                    val cached = receipts.saved(appointmentId)
                    _state.value = if (cached != null) UiState.Success(AppointmentDetail(null, cached)) else UiState.Error(result.error)
                }
            }
        }
    }

    fun cancel() = guarded {
        when (val result = appointments.cancel(appointmentId)) {
            is ApiResult.Success -> { _events.emit(DetailEvent.Cancelled); load() }
            is ApiResult.Failure -> _events.emit(DetailEvent.Failed(result.error))
        }
    }

    fun review(rating: Int, text: String?) {
        if (rating < 1) { _events.tryEmit(DetailEvent.RatingMissing); return }
        guarded {
            when (val result = appointments.review(appointmentId, rating, text)) {
                is ApiResult.Success -> { _events.emit(DetailEvent.Reviewed); load() }
                is ApiResult.Failure -> _events.emit(DetailEvent.Failed(result.error))
            }
        }
    }

    /** Step 1 of the optional deposit: ask the backend for a Razorpay order. */
    fun startDeposit() = guarded {
        when (val result = payments.createOrder(appointmentId)) {
            is ApiResult.Success -> _events.emit(DetailEvent.OpenCheckout(result.data))
            is ApiResult.Failure -> _events.emit(DetailEvent.Failed(result.error))
        }
    }

    private fun onPaymentResult(result: PaymentResult) {
        when (result) {
            is PaymentResult.Failure -> _events.tryEmit(DetailEvent.PaymentFailed(result.description))
            is PaymentResult.Success -> guarded {
                // The backend recomputes the signature; only its verdict marks the deposit as paid.
                val request = VerifyPaymentRequest(result.orderId, result.paymentId, result.signature)
                when (val verified = payments.verify(appointmentId, request)) {
                    is ApiResult.Success -> { _events.emit(DetailEvent.DepositPaid); load() }
                    is ApiResult.Failure -> _events.emit(DetailEvent.Failed(verified.error))
                }
            }
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
