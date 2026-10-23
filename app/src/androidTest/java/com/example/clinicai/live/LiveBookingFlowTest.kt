package com.example.clinicai.live

import android.content.Intent
import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.clinicai.R
import com.example.clinicai.data.auth.AuthOutcome
import com.example.clinicai.data.auth.AuthState
import com.example.clinicai.data.model.BookAppointmentRequest
import com.example.clinicai.data.model.Doctor
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.domain.BookingDates
import com.example.clinicai.live.LiveTestSupport.TEST_CODE
import com.example.clinicai.live.LiveTestSupport.assumeLive
import com.example.clinicai.live.LiveTestSupport.container
import com.example.clinicai.live.LiveTestSupport.newEmail
import com.example.clinicai.live.LiveTestSupport.newPassword
import com.example.clinicai.live.LiveTestSupport.waitFor
import com.example.clinicai.ui.main.MainActivity
import com.example.clinicai.util.DateFormats
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The booking journey through the real screens: real doctors and availability from the backend,
 * a real Clerk session, a real appointment row, and the receipt built from the server's answer.
 */
@RunWith(AndroidJUnit4::class)
class LiveBookingFlowTest {

    private data class Target(val doctor: Doctor, val dayOffset: Int, val date: String, val slot: String)

    private val created = mutableListOf<String>()

    @Before fun setUp() {
        assumeLive()
        runBlocking { container.auth.signOut() }
        waitFor { assertTrue(container.auth.authState.value is AuthState.SignedOut) }
    }

    @After fun tearDown() {
        if (LiveTestSupport.args.getString("liveAuth") != "true") return
        // Leave no confirmed test appointments behind.
        runBlocking {
            created.forEach { container.appointments.cancel(it) }
            container.auth.signOut()
        }
    }

    private fun registerAndSignIn(): String {
        val email = newEmail("booker")
        runBlocking {
            val step = container.auth.signUp("Booking", "Tester", email, newPassword())
            check(step is AuthOutcome.NeedsCode) { "expected a verification step, got $step" }
            val done = container.auth.submitCode(TEST_CODE)
            check(done is AuthOutcome.SignedIn) { "sign-up did not complete: $done" }
        }
        waitFor { assertTrue(container.auth.authState.value is AuthState.SignedIn) }
        return email
    }

    /** A doctor with free slots, found through the same API the app uses. */
    private fun findTarget(minSlots: Int = 3): Target = runBlocking {
        val doctors = (container.doctors.allDoctors() as ApiResult.Success).data
        for (doctor in doctors) {
            BookingDates.next(14).forEachIndexed { offset, day ->
                val slots = (container.doctors.availability(doctor.id, day.iso) as? ApiResult.Success)?.data?.slots.orEmpty()
                if (slots.size >= minSlots) return@runBlocking Target(doctor, offset, day.iso, slots[1])
            }
        }
        error("no doctor with free slots in the next 14 days")
    }

    private fun openDoctorScreen(target: Target): ActivityScenario<MainActivity> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://clinicai-555888.vercel.app/doctors/${target.doctor.id}"))
            .setClassName(context, MainActivity::class.java.name)
        val scenario = ActivityScenario.launch<MainActivity>(intent)
        waitFor { onView(withText(R.string.book_title)).check(matches(isDisplayed())) }
        return scenario
    }

    private fun selectDayAndSlot(target: Target) {
        onView(withId(R.id.dateStrip)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(target.dayOffset, click()))
        val label = DateFormats.time12h(target.slot)
        waitFor { onView(withText(label)).perform(scrollTo()).check(matches(isDisplayed())) }
        onView(withText(label)).perform(click())
        onView(withId(R.id.reviewButton)).check(matches(isEnabled()))
    }

    // -------------------------------------------------------------------------------------------

    @Test fun signedInUserBooksAnAppointmentAndGetsAReceiptForIt() {
        val email = registerAndSignIn()
        val target = findTarget()

        openDoctorScreen(target).use {
            selectDayAndSlot(target)
            onView(withId(R.id.reviewButton)).perform(click())

            // Requirement 9: a signed-in user is never told to sign in on the review screen.
            waitFor { onView(withText(R.string.book_review_title)).check(matches(isDisplayed())) }
            waitFor { onView(withId(R.id.confirmButton)).check(matches(withText(R.string.book_confirm))) }
            onView(withId(R.id.signInHint)).check(matches(not(isDisplayed())))
            onView(withText(containsString(email))).check(matches(isDisplayed())) // patient shown on the review

            onView(withId(R.id.confirmButton)).perform(click())

            // The confirmation exists only because the server created the appointment.
            waitFor { onView(withText(R.string.confirm_title)).check(matches(isDisplayed())) }
            val appointments = (runBlocking { container.appointments.list() } as ApiResult.Success).data
            val booked = appointments.single { it.doctorId == target.doctor.id && it.dateOnly == target.date && it.startTime == target.slot }
            created += booked.id
            assertEquals("confirmed", booked.status)
            onView(withText(booked.id)).check(matches(isDisplayed())) // the server's booking id, verbatim

            // Receipt screen shows the same real record.
            onView(withId(R.id.viewReceipt)).perform(click())
            waitFor { onView(withId(R.id.receiptTitle)).check(matches(withText("Booking Confirmation"))) }
            onView(withText(booked.id)).check(matches(isDisplayed()))
            onView(withText(containsString(target.doctor.name))).check(matches(isDisplayed()))
            onView(withText("No online payment taken")).perform(scrollTo()).check(matches(isDisplayed()))

            // The slot is no longer offered, according to the server.
            val after = (runBlocking { container.doctors.availability(target.doctor.id, target.date) } as ApiResult.Success).data.slots
            assertTrue("booked slot must disappear from availability", target.slot !in after)
        }
    }

    @Test fun signedOutUserIsAskedToSignInAndKeepsTheirSelection() {
        val target = findTarget()
        openDoctorScreen(target).use {
            selectDayAndSlot(target)
            onView(withId(R.id.reviewButton)).perform(click())
            waitFor { onView(withText(R.string.book_review_title)).check(matches(isDisplayed())) }
            waitFor { onView(withId(R.id.confirmButton)).check(matches(withText(R.string.book_sign_in_to_confirm))) }
            onView(withId(R.id.signInHint)).check(matches(isDisplayed()))
            // The draft is still on screen while the user would sign in.
            onView(withText(containsString(target.doctor.name))).check(matches(isDisplayed()))
        }
    }

    /**
     * Regression test for "the review page shows a sign-in message even though I'm signed in":
     * the screen used to sample the session once. It must follow the session as it changes,
     * which is also what happens when a saved session is restored a moment after launch.
     */
    @Test fun reviewScreenFollowsTheSessionWithoutBeingReopened() {
        val target = findTarget()
        openDoctorScreen(target).use {
            selectDayAndSlot(target)
            onView(withId(R.id.reviewButton)).perform(click())
            waitFor { onView(withId(R.id.confirmButton)).check(matches(withText(R.string.book_sign_in_to_confirm))) }
            onView(withId(R.id.signInHint)).check(matches(isDisplayed()))

            registerAndSignIn() // the session becomes active while the screen is open

            waitFor { onView(withId(R.id.confirmButton)).check(matches(withText(R.string.book_confirm))) }
            onView(withId(R.id.signInHint)).check(matches(not(isDisplayed())))
            onView(withId(R.id.confirmButton)).check(matches(isEnabled()))
            // ...and signing out again brings the prompt back, still without reopening the screen.
            runBlocking { container.auth.signOut() }
            waitFor { onView(withId(R.id.confirmButton)).check(matches(withText(R.string.book_sign_in_to_confirm))) }
        }
    }

    @Test fun aSlotTakenWhileReviewingIsRejectedByTheServerAndNothingIsConfirmed() {
        registerAndSignIn()
        val target = findTarget()
        openDoctorScreen(target).use {
            selectDayAndSlot(target)
            onView(withId(R.id.reviewButton)).perform(click())
            waitFor { onView(withId(R.id.confirmButton)).check(matches(withText(R.string.book_confirm))) }

            // Someone else takes the slot while this user is still reviewing.
            val taken = runBlocking {
                container.appointments.book(BookAppointmentRequest(target.doctor.id, target.date, target.slot, "taken first"))
            }
            assertTrue("setup booking must succeed: $taken", taken is ApiResult.Success)
            created += (taken as ApiResult.Success).data.id

            onView(withId(R.id.confirmButton)).perform(click())
            waitFor { onView(withText(containsString("Please pick another time"))).check(matches(isDisplayed())) }
            onView(withId(R.id.chooseAnotherButton)).check(matches(isDisplayed()))
            // No confirmation, no receipt.
            onView(withText(R.string.confirm_title)).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())
            val mine = (runBlocking { container.appointments.list() } as ApiResult.Success).data
                .filter { it.doctorId == target.doctor.id && it.dateOnly == target.date && it.startTime == target.slot && it.status == "confirmed" }
            assertEquals("exactly one confirmed appointment may hold the slot", 1, mine.size)

            // "Choose another time" returns to the doctor, whose slot list has been refreshed.
            onView(withId(R.id.chooseAnotherButton)).perform(click())
            waitFor { onView(withText(R.string.book_title)).check(matches(isDisplayed())) }
            val label = DateFormats.time12h(target.slot)
            waitFor { onView(withText(label)).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist()) }
        }
    }
}
