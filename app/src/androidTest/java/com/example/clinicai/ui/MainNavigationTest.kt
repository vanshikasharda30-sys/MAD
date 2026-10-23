package com.example.clinicai.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clinicai.R
import com.example.clinicai.ui.main.MainActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainNavigationTest {

    /** A title shown on screen, not the identically named bottom-navigation label. */
    private fun screenText(res: Int) = allOf(withText(res), not(isDescendantOfA(withId(R.id.bottomNav))))


    @Test fun launchesOnTheNativeHomeScreen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.heroFind)).check(matches(isDisplayed()))
            onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))
        }
    }

    @Test fun bottomNavigationReachesEveryTopLevelScreen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.findDoctorFragment)).perform(click())
            onView(screenText(R.string.find_title)).check(matches(isDisplayed()))

            onView(withId(R.id.chatFragment)).perform(click())
            onView(screenText(R.string.chat_greeting)).check(matches(isDisplayed()))

            onView(withId(R.id.myAppointmentsFragment)).perform(click())
            onView(screenText(R.string.appts_title)).check(matches(isDisplayed()))

            onView(withId(R.id.profileFragment)).perform(click())
            onView(screenText(R.string.profile_title)).check(matches(isDisplayed()))

            onView(withId(R.id.homeFragment)).perform(click())
            onView(screenText(R.string.home_headline)).check(matches(isDisplayed()))
        }
    }

    @Test fun signedOutVisitsScreenAsksToSignIn() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.myAppointmentsFragment)).perform(click())
            onView(withText(R.string.appts_sign_in_title)).check(matches(isDisplayed()))
            onView(withId(R.id.signInButton)).check(matches(isDisplayed()))
        }
    }

    @Test fun signedOutAccountScreenOffersSignIn() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.profileFragment)).perform(click())
            onView(withText(R.string.profile_signed_out_title)).check(matches(isDisplayed()))
        }
    }

    @Test fun homeQuickActionsAreWired() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.actionWalkIn)).perform(click())
            onView(withText(R.string.checkin_scan)).check(matches(isDisplayed()))
        }
    }
}
