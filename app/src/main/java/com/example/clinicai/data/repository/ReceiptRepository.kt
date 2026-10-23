package com.example.clinicai.data.repository

import com.example.clinicai.data.auth.ClerkAuthManager
import com.example.clinicai.data.local.ReceiptStore
import com.example.clinicai.data.model.Appointment
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.domain.PatientInfo
import com.example.clinicai.domain.ReceiptBuilder
import com.example.clinicai.domain.ReceiptRecorder
import com.example.clinicai.domain.ReceiptData
import kotlinx.coroutines.withTimeoutOrNull

/** Builds receipts from backend-confirmed appointments and keeps them for the signed-in user. */
class ReceiptRepository(
    private val store: ReceiptStore,
    private val auth: ClerkAuthManager,
    private val users: UserRepository,
) : ReceiptRecorder {
    override suspend fun patientInfo(): PatientInfo {
        val account = auth.accountInfo()
        // The booking is already confirmed when this runs; never let a slow profile call hold up the UI.
        val profile = withTimeoutOrNull(PROFILE_TIMEOUT_MS) {
            (users.me() as? com.example.clinicai.data.remote.ApiResult.Success)?.data
        }
        return PatientInfo(
            name = profile?.name?.takeIf { it.isNotBlank() } ?: account?.displayName?.takeIf { it.isNotBlank() },
            email = profile?.email ?: account?.email,
            phone = profile?.phone?.takeIf { it.isNotBlank() },
        )
    }

    /** Build and persist the receipt for a booking the backend just confirmed. */
    override suspend fun recordConfirmed(appointment: Appointment, doctor: Doctor?, patient: PatientInfo): ReceiptData {
        val receipt = ReceiptBuilder.build(appointment, doctor, patient)
        auth.currentUserId?.let { store.save(it, receipt) }
        return receipt
    }

    suspend fun saved(appointmentId: String): ReceiptData? =
        auth.currentUserId?.let { store.find(it, appointmentId) }

    suspend fun allSaved(): List<ReceiptData> =
        auth.currentUserId?.let { store.all(it) } ?: emptyList()

    private companion object {
        const val PROFILE_TIMEOUT_MS = 4_000L
    }
}
