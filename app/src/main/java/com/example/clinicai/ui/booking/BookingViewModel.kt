package com.example.clinicai.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clinicai.data.model.BookAppointmentRequest
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.data.auth.AuthState
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.domain.AccountProvider
import com.example.clinicai.domain.AppointmentBooker
import com.example.clinicai.domain.ReceiptData
import com.example.clinicai.domain.ReceiptRecorder
import com.example.clinicai.domain.SymptomNotesStore
import com.example.clinicai.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The slot the patient picked on the doctor screen, waiting to be reviewed and confirmed. */
data class BookingDraft(val doctor: Doctor, val date: String, val startTime: String)

sealed interface BookingState {
    data object Idle : BookingState
    data object Submitting : BookingState

    /** Reached ONLY after the backend answered 201 with a created appointment. */
    data class Confirmed(val receipt: ReceiptData) : BookingState

    /**
     * The booking was not confirmed. [outcomeUnknown] is true for timeouts / dropped connections,
     * where the server may or may not have created the appointment.
     */
    data class Failed(val error: AppError?, val formProblem: Validators.BookingCheck? = null, val outcomeUnknown: Boolean = false) : BookingState
}

/**
 * Shared by the doctor, review and confirmation screens. The single guard in [submit] (state ==
 * Submitting) makes repeated taps harmless: only one request is ever in flight.
 */
class BookingViewModel(
    private val booker: AppointmentBooker,
    private val receipts: ReceiptRecorder,
    private val notesStore: SymptomNotesStore,
    private val account: AccountProvider,
) : ViewModel() {

    private val _draft = MutableStateFlow<BookingDraft?>(null)
    val draft: StateFlow<BookingDraft?> = _draft.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _state = MutableStateFlow<BookingState>(BookingState.Idle)
    val state: StateFlow<BookingState> = _state.asStateFlow()

    val isSignedIn: Boolean get() = account.isSignedIn

    /** Reactive: `Initializing` while the auth SDK restores a saved session, then SignedIn / SignedOut. */
    val authState: StateFlow<AuthState> get() = account.authState
    fun accountName(): String? = account.accountInfo()?.displayName?.takeIf { it.isNotBlank() }
    fun accountEmail(): String? = account.accountInfo()?.email

    fun startDraft(doctor: Doctor, date: String, startTime: String) {
        if (_state.value is BookingState.Submitting) return
        _draft.value = BookingDraft(doctor, date, startTime)
        _state.value = BookingState.Idle
        // Symptom text from the chat is prefilled, as on the website.
        viewModelScope.launch { _notes.value = notesStore.readSymptomNotes().orEmpty() }
    }

    private var slotsStale = false

    /**
     * True once if the slot list shown earlier is out of date: a booking was just confirmed, or the
     * server said the slot was taken. The doctor screen reloads availability when it sees this.
     */
    fun consumeSlotsStale(): Boolean = slotsStale.also { slotsStale = false }

    fun setNotes(value: String) {
        _notes.value = value
    }

    fun submit() {
        if (_state.value is BookingState.Submitting) return // duplicate tap
        val draft = _draft.value ?: return
        val notes = _notes.value.trim()

        val check = Validators.checkBooking(draft.date, draft.startTime, notes)
        if (check != Validators.BookingCheck.Ok) {
            _state.value = BookingState.Failed(error = null, formProblem = check)
            return
        }

        _state.value = BookingState.Submitting
        viewModelScope.launch {
            val request = BookAppointmentRequest(
                doctorId = draft.doctor.id,
                date = draft.date,
                startTime = draft.startTime,
                notes = notes.takeIf { it.isNotEmpty() },
            )
            when (val result = booker.book(request)) {
                is ApiResult.Success -> {
                    // The booking exists on the server from this point on. Anything below that
                    // goes wrong (profile lookup, local receipt cache) must not undo the confirmation.
                    val patient = receipts.patientInfo()
                    val receipt = receipts.recordConfirmed(result.data, draft.doctor, patient)
                    notesStore.clearSymptomNotes()
                    slotsStale = true
                    _state.value = BookingState.Confirmed(receipt)
                }
                is ApiResult.Failure -> {
                    if (result.error is AppError.Conflict) slotsStale = true
                    // Any failure that happened after the request may have left the device could still have created the booking.
                    val unknown = result.error is AppError.Timeout || result.error is AppError.Offline ||
                        result.error is AppError.ServerUnreachable || result.error is AppError.SecureConnectionFailed
                    _state.value = BookingState.Failed(result.error, outcomeUnknown = unknown)
                }
            }
        }
    }

    /** Clears a finished/failed attempt so the review screen can be used again. */
    fun dismissFailure() {
        if (_state.value is BookingState.Failed) _state.value = BookingState.Idle
    }

    fun reset() {
        if (_state.value is BookingState.Submitting) return
        _draft.value = null
        _notes.value = ""
        _state.value = BookingState.Idle
    }
}
