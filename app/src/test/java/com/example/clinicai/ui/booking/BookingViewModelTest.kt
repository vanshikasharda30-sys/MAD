package com.example.clinicai.ui.booking

import com.example.clinicai.data.auth.AccountInfo
import com.example.clinicai.data.auth.AuthState
import com.example.clinicai.data.model.Appointment
import com.example.clinicai.data.model.BookAppointmentRequest
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.data.remote.AppError
import com.example.clinicai.domain.AccountProvider
import com.example.clinicai.domain.AppointmentBooker
import com.example.clinicai.domain.PatientInfo
import com.example.clinicai.domain.ReceiptBuilder
import com.example.clinicai.domain.ReceiptData
import com.example.clinicai.domain.ReceiptRecorder
import com.example.clinicai.domain.SymptomNotesStore
import com.example.clinicai.testing.Fixtures
import com.example.clinicai.util.Validators
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The core guarantee of the booking flow: [BookingState.Confirmed] exists only after the backend
 * answered with a created appointment. Every other outcome must leave the state as Failed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookingViewModelTest {

    private class FakeBooker(var next: suspend () -> ApiResult<Appointment>) : AppointmentBooker {
        var calls = 0
        var lastRequest: BookAppointmentRequest? = null
        override suspend fun book(request: BookAppointmentRequest): ApiResult<Appointment> {
            calls++
            lastRequest = request
            return next()
        }
    }

    private class FakeRecorder : ReceiptRecorder {
        var recorded: ReceiptData? = null
        override suspend fun patientInfo() = PatientInfo("Jane Doe", "jane@example.com", null)
        override suspend fun recordConfirmed(appointment: Appointment, doctor: Doctor?, patient: PatientInfo) =
            ReceiptBuilder.build(appointment, doctor, patient).also { recorded = it }
    }

    private class FakeNotes(var notes: String? = "chat symptoms") : SymptomNotesStore {
        var cleared = false
        override suspend fun readSymptomNotes() = notes
        override suspend fun clearSymptomNotes() { cleared = true; notes = null }
    }

    private class FakeAccount(signedIn: Boolean = true) : AccountProvider {
        override val isSignedIn = signedIn
        override val authState = kotlinx.coroutines.flow.MutableStateFlow<AuthState>(
            if (signedIn) AuthState.SignedIn("user_1") else AuthState.SignedOut,
        )
        override suspend fun awaitReady(): AuthState = authState.value
        override fun accountInfo() = AccountInfo("Jane", "Doe", "jane@example.com")
    }

    private val recorder = FakeRecorder()
    private val notes = FakeNotes()

    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(booker: FakeBooker) =
        BookingViewModel(booker, recorder, notes, FakeAccount()).also {
            it.startDraft(Fixtures.doctor, "2026-10-26", "10:00")
        }

    private val created = Fixtures.appointment(id = "srv_123")

    @Test fun confirmedOnlyAfterTheServerCreatesTheAppointment() = runTest {
        val booker = FakeBooker { ApiResult.Success(created) }
        val vm = viewModel(booker)
        advanceUntilIdle()
        assertTrue("nothing is confirmed before submitting", vm.state.value is BookingState.Idle)

        vm.submit()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is BookingState.Confirmed)
        assertEquals("srv_123", (state as BookingState.Confirmed).receipt.appointmentId)
        assertEquals("Dr. Ada Test", state.receipt.doctorName)
        assertTrue("symptom notes are consumed by a confirmed booking", notes.cleared)
        assertEquals("srv_123", recorder.recorded?.appointmentId)
    }

    @Test fun requestCarriesTheDraftAndPrefilledNotes() = runTest {
        val booker = FakeBooker { ApiResult.Success(created) }
        val vm = viewModel(booker)
        advanceUntilIdle()
        vm.submit()
        advanceUntilIdle()
        assertEquals(BookAppointmentRequest("doc_t", "2026-10-26", "10:00", "chat symptoms"), booker.lastRequest)
    }

    @Test fun slotTakenIsNeverConfirmed() = runTest {
        val booker = FakeBooker { ApiResult.Failure(AppError.Validation("That slot was just booked by someone else")) }
        val vm = viewModel(booker)
        advanceUntilIdle()
        vm.submit()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is BookingState.Failed)
        assertTrue((state as BookingState.Failed).error is AppError.Validation)
        assertFalse(state.outcomeUnknown)
        assertNull("no receipt may exist for an unconfirmed booking", recorder.recorded)
        assertFalse(notes.cleared)
    }

    @Test fun unauthorizedIsNeverConfirmed() = runTest {
        val vm = viewModel(FakeBooker { ApiResult.Failure(AppError.Unauthorized()) })
        advanceUntilIdle()
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.state.value is BookingState.Failed)
        assertNull(recorder.recorded)
    }

    @Test fun timeoutAndOfflineAreFlaggedAsUnknownOutcome() = runTest {
        listOf(AppError.Timeout, AppError.Offline, AppError.ServerUnreachable, AppError.SecureConnectionFailed).forEach { error ->
            val vm = viewModel(FakeBooker { ApiResult.Failure(error) })
            advanceUntilIdle()
            vm.submit()
            advanceUntilIdle()
            val state = vm.state.value as BookingState.Failed
            assertTrue("$error must warn that the booking may exist", state.outcomeUnknown)
        }
        assertNull(recorder.recorded)
    }

    @Test fun repeatedTapsSendExactlyOneRequest() = runTest {
        val gate = CompletableDeferred<ApiResult<Appointment>>()
        val booker = FakeBooker { gate.await() }
        val vm = viewModel(booker)
        advanceUntilIdle()

        vm.submit()
        vm.submit()
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.state.value is BookingState.Submitting)
        assertEquals(1, booker.calls)

        gate.complete(ApiResult.Success(created))
        advanceUntilIdle()
        assertTrue(vm.state.value is BookingState.Confirmed)
        assertEquals(1, booker.calls)
    }

    @Test fun overlongNotesAreRejectedBeforeAnyRequest() = runTest {
        val booker = FakeBooker { ApiResult.Success(created) }
        val vm = viewModel(booker)
        advanceUntilIdle()
        vm.setNotes("x".repeat(501))
        vm.submit()
        advanceUntilIdle()

        val state = vm.state.value as BookingState.Failed
        assertEquals(Validators.BookingCheck.NotesTooLong, state.formProblem)
        assertEquals(0, booker.calls)
    }

    @Test fun submitWithoutADraftDoesNothing() = runTest {
        val booker = FakeBooker { ApiResult.Success(created) }
        val vm = BookingViewModel(booker, recorder, notes, FakeAccount())
        vm.submit()
        advanceUntilIdle()
        assertEquals(0, booker.calls)
        assertTrue(vm.state.value is BookingState.Idle)
    }

    @Test fun canRetryAfterAFailure() = runTest {
        var attempt = 0
        val booker = FakeBooker {
            if (++attempt == 1) ApiResult.Failure(AppError.Validation("slot taken")) else ApiResult.Success(created)
        }
        val vm = viewModel(booker)
        advanceUntilIdle()
        vm.submit(); advanceUntilIdle()
        assertTrue(vm.state.value is BookingState.Failed)
        vm.dismissFailure()
        vm.submit(); advanceUntilIdle()
        assertTrue(vm.state.value is BookingState.Confirmed)
        assertEquals(2, booker.calls)
    }

    @Test fun resetClearsTheDraftSoNoStaleBookingSurvives() = runTest {
        val vm = viewModel(FakeBooker { ApiResult.Success(created) })
        advanceUntilIdle()
        vm.submit(); advanceUntilIdle()
        vm.reset()
        assertNull(vm.draft.value)
        assertTrue(vm.state.value is BookingState.Idle)
    }

    @Test fun conflictIsNeverConfirmedAndMarksTheSlotListStale() = runTest {
        val booker = FakeBooker { ApiResult.Failure(AppError.Conflict("That slot was just booked by someone else")) }
        val vm = viewModel(booker)
        advanceUntilIdle()
        assertFalse(vm.consumeSlotsStale())

        vm.submit()
        advanceUntilIdle()

        val state = vm.state.value as BookingState.Failed
        assertTrue(state.error is AppError.Conflict)
        assertFalse("a conflict is a definite 'no', not an unknown outcome", state.outcomeUnknown)
        assertNull("no receipt for a failed booking", recorder.recorded)
        assertTrue("the doctor screen must reload availability", vm.consumeSlotsStale())
        assertFalse("the flag is consumed once", vm.consumeSlotsStale())
    }

    @Test fun aConfirmedBookingAlsoMarksTheSlotListStale() = runTest {
        val vm = viewModel(FakeBooker { ApiResult.Success(created) })
        advanceUntilIdle()
        vm.submit(); advanceUntilIdle()
        assertTrue(vm.consumeSlotsStale())
    }

    @Test fun authStateIsExposedReactively() = runTest {
        val account = FakeAccount(signedIn = false)
        val vm = BookingViewModel(FakeBooker { ApiResult.Success(created) }, recorder, notes, account)
        assertTrue(vm.authState.value is AuthState.SignedOut)
        account.authState.value = AuthState.SignedIn("user_1")
        assertTrue("signing in must be visible without recreating the screen", vm.authState.value is AuthState.SignedIn)
    }
}
