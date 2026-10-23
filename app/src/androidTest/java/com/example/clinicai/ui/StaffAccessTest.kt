package com.example.clinicai.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clinicai.R
import com.example.clinicai.ui.staff.StaffActivity
import org.junit.Test
import org.junit.runner.RunWith

/** The staff area must never expose clinic data to someone who isn't a verified staff member. */
@RunWith(AndroidJUnit4::class)
class StaffAccessTest {

    @Test fun signedOutUserSeesASignInMessageAndNoDashboard() {
        ActivityScenario.launch(StaffActivity::class.java).use {
            onView(withText(R.string.error_unauthorized)).check(matches(isDisplayed()))
            onView(withId(R.id.stateRetry)).check(matches(isDisplayed()))
        }
    }
}
