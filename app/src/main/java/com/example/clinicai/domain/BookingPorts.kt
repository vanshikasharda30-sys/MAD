package com.example.clinicai.domain

import com.example.clinicai.data.auth.AccountInfo
import com.example.clinicai.data.model.Appointment
import com.example.clinicai.data.model.BookAppointmentRequest
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.data.remote.ApiResult

/** Sends a booking to the backend. A Success means the server created the appointment. */
interface AppointmentBooker {
    suspend fun book(request: BookAppointmentRequest): ApiResult<Appointment>
}

/** Builds and keeps the receipt for a booking the server confirmed. */
interface ReceiptRecorder {
    suspend fun patientInfo(): PatientInfo
    suspend fun recordConfirmed(appointment: Appointment, doctor: Doctor?, patient: PatientInfo): ReceiptData
}

/** The signed-in account, as far as the booking flow needs to know. */
interface AccountProvider {
    val isSignedIn: Boolean

    /** Reactive sign-in state; `Initializing` until the auth SDK has restored any saved session. */
    val authState: kotlinx.coroutines.flow.StateFlow<com.example.clinicai.data.auth.AuthState>
    suspend fun awaitReady(): com.example.clinicai.data.auth.AuthState
    fun accountInfo(): AccountInfo?
}

/** Symptom text carried over from the chat into the booking notes. */
interface SymptomNotesStore {
    suspend fun readSymptomNotes(): String?
    suspend fun clearSymptomNotes()
}
