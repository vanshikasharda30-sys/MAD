package com.example.clinicai.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clinicai.R
import com.example.clinicai.ui.main.MainActivity
import java.util.concurrent.TimeUnit
import org.hamcrest.Matchers.containsString
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real deployed backend (public, read-only endpoints plus one chat message). Skipped when offline. */
@RunWith(AndroidJUnit4::class)
class LiveBackendTest {

    @Before fun setUp() {
        NetworkAssumptions.assumeBackendReachable()
        IdlingPolicies.setMasterPolicyTimeout(30, TimeUnit.SECONDS)
    }

    private fun waitFor(timeoutMs: Long = 15_000, assertion: () -> Unit) {
        val end = System.currentTimeMillis() + timeoutMs
        var last: Throwable? = null
        while (System.currentTimeMillis() < end) {
            try { assertion(); return } catch (e: Throwable) { last = e; Thread.sleep(300) }
        }
        throw last ?: AssertionError("timed out")
    }

    private fun recyclerHasItems() = ViewAssertion { view, error ->
        if (view == null) throw NoMatchingViewException.Builder().includeViewHierarchy(true).withRootView(null).build()
        val count = (view as RecyclerView).adapter?.itemCount ?: 0
        if (count <= 0) throw AssertionError("RecyclerView is empty", error)
    }

    @Test fun findDoctorListsRealDoctorsFromTheBackend() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.findDoctorFragment)).perform(click())
            waitFor { onView(withId(R.id.doctorList)).check(recyclerHasItems()) }
        }
    }

    @Test fun searchFiltersTheDirectory() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.findDoctorFragment)).perform(click())
            waitFor { onView(withId(R.id.doctorList)).check(recyclerHasItems()) }
            onView(withId(R.id.searchInput)).perform(replaceText("zzzz-no-such-doctor"), closeSoftKeyboard())
            waitFor { onView(withText(R.string.find_empty_title)).check(matches(isDisplayed())) }
        }
    }

    @Test fun doctorDetailShowsBookingCard() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.findDoctorFragment)).perform(click())
            waitFor { onView(withId(R.id.doctorList)).check(recyclerHasItems()) }
            onView(withId(R.id.doctorList)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
            waitFor { onView(withText(R.string.book_title)).check(matches(isDisplayed())) }
        }
    }

    @Test fun chatReceivesAReplyFromTheBackend() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.chatFragment)).perform(click())
            onView(withId(R.id.messageInput)).perform(replaceText("I have a fever and cough"), closeSoftKeyboard())
            onView(withId(R.id.sendButton)).perform(click())
            onView(withText("I have a fever and cough")).check(matches(isDisplayed()))
            waitFor { onView(withText(containsString("General Physician"))).check(matches(isDisplayed())) }
        }
    }

    @Test fun invalidCheckinCodeIsReportedNotCrashed() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.actionWalkIn)).perform(click())
            onView(withId(R.id.codeInput)).perform(replaceText("definitely-not-a-real-token"), closeSoftKeyboard())
            onView(withId(R.id.lookupButton)).perform(click())
            waitFor { onView(withText(R.string.checkin_invalid_code)).check(matches(isDisplayed())) }
        }
    }

    @Test fun faqScreenShowsClinicFaqs() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.actionFaq)).perform(click())
            waitFor { onView(withId(R.id.faqList)).check(recyclerHasItems()) }
        }
    }
}
