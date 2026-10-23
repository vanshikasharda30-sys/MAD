package com.example.clinicai.live

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.clinicai.R
import com.example.clinicai.data.auth.AuthOutcome
import com.example.clinicai.data.auth.AuthState
import com.example.clinicai.live.LiveTestSupport.TEST_CODE
import com.example.clinicai.live.LiveTestSupport.arg
import com.example.clinicai.live.LiveTestSupport.assumeLive
import com.example.clinicai.live.LiveTestSupport.container
import com.example.clinicai.live.LiveTestSupport.waitFor
import com.example.clinicai.ui.staff.StaffActivity
import kotlinx.coroutines.runBlocking
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The staff area for a real staff account (needs a backend that has the dashboard endpoints). */
@RunWith(AndroidJUnit4::class)
class LiveStaffAreaTest {

    @Before fun setUp() {
        assumeLive()
        runBlocking {
            container.auth.signOut()
            val step = container.auth.signIn(arg("staffEmail"), arg("staffPassword"))
            if (step is AuthOutcome.NeedsCode) container.auth.submitCode(TEST_CODE)
        }
        waitFor { assertTrue(container.auth.authState.value is AuthState.SignedIn) }
    }

    @After fun tearDown() {
        if (LiveTestSupport.args.getString("liveAuth") == "true") runBlocking { container.auth.signOut() }
    }

    private fun openDrawerItem(id: Int) {
        onView(withId(R.id.drawerLayout)).perform(DrawerActions.open())
        onView(withId(R.id.navigationView)).perform(NavigationViewActions.navigateTo(id))
    }

    @Test fun staffSeesTheirClinicAcrossEveryDashboardScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ActivityScenario.launch<StaffActivity>(Intent(context, StaffActivity::class.java)).use { scenario ->
            // Overview
            try {
                waitFor { onView(withText(containsString("Lilavati"))).check(matches(isDisplayed())) }
            } catch (e: Throwable) {
                // Say what the screen showed instead, so a failure is diagnosable rather than just "not found".
                var shown = "(activity gone)"
                scenario.onActivity { shown = it.findViewById<android.widget.TextView>(R.id.stateErrorMessage)?.text?.toString() ?: "(no error text)" }
                throw AssertionError("Staff overview never showed the clinic. Gate message: \"$shown\"; auth=${container.auth.authState.value}", e)
            }
            onView(withText(R.string.staff_active_doctors)).check(matches(isDisplayed()))
            onView(withText(R.string.staff_waiting_queue)).check(matches(isDisplayed()))
            // The website's All / Scheduled / Walk-in tabs, filled from the server's real list.
            onView(withId(R.id.tabAll)).perform(androidx.test.espresso.action.ViewActions.scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.tabScheduled)).check(matches(isDisplayed()))
            onView(withId(R.id.tabWalkIn)).check(matches(isDisplayed()))
            // The appointment a real patient booked through the API shows up for this clinic's staff.
            waitFor { onView(withText(containsString(arg("bookedRef")))).perform(androidx.test.espresso.action.ViewActions.scrollTo()).check(matches(isDisplayed())) }
            // Patient and doctor must be in the same row as that reference (earlier runs leave similar rows behind).
            val sameRow = allOf(withId(R.id.appointmentRow), hasDescendant(withText(containsString(arg("bookedRef")))))
            onView(allOf(withId(R.id.patientName), isDescendantOfA(sameRow), withText(containsString(arg("bookedPatient"))))).perform(androidx.test.espresso.action.ViewActions.scrollTo()).check(matches(isDisplayed()))
            onView(allOf(withId(R.id.doctorName), isDescendantOfA(sameRow), withText(containsString(arg("bookedDoctor"))))).perform(androidx.test.espresso.action.ViewActions.scrollTo()).check(matches(isDisplayed()))
            // Switching to Today and back reloads from the server and still shows it.
            onView(withId(R.id.scopeToday)).perform(androidx.test.espresso.action.ViewActions.scrollTo(), click())
            waitFor { onView(withId(R.id.scopeToday)).check(matches(androidx.test.espresso.matcher.ViewMatchers.isChecked())) }
            onView(withId(R.id.scopeUpcoming)).perform(androidx.test.espresso.action.ViewActions.scrollTo(), click())
            waitFor { onView(withText(containsString(arg("bookedRef")))).perform(androidx.test.espresso.action.ViewActions.scrollTo()).check(matches(isDisplayed())) }
            // The "needs a backend update" message must be gone.
            onView(withText(R.string.error_not_deployed)).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist())

            // Doctors (admin sees the add button)
            openDrawerItem(R.id.staffDoctorsFragment)
            waitFor { onView(withId(R.id.addDoctorButton)).check(matches(isDisplayed())) }

            // Live queue
            openDrawerItem(R.id.staffQueueFragment)
            waitFor { onView(withText(R.string.queue_currently_serving)).check(matches(isDisplayed())) }

            // Analytics
            openDrawerItem(R.id.staffAnalyticsFragment)
            waitFor { onView(withText(R.string.analytics_trend)).check(matches(isDisplayed())) }

            // Staff list, including this admin, with the invite form
            openDrawerItem(R.id.staffMembersFragment)
            // Identify this account by its unique e-mail, since earlier runs may have left other "QA Admin" rows.
            waitFor { onView(withText(arg("staffEmail").lowercase())).check(matches(isDisplayed())) }
            onView(withId(R.id.inviteButton)).check(matches(isDisplayed()))

            // Clinic settings
            openDrawerItem(R.id.clinicSettingsFragment)
            waitFor { onView(withId(R.id.saveButton)).check(matches(isDisplayed())) }
        }
    }
}
