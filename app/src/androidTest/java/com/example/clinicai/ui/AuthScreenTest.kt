package com.example.clinicai.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clinicai.R
import com.example.clinicai.testing.hasInputError
import com.example.clinicai.ui.auth.AuthActivity
import org.hamcrest.Matchers.containsString
import org.junit.Test
import org.junit.runner.RunWith

/** Client-side validation: nothing here reaches the network because invalid forms are never submitted. */
@RunWith(AndroidJUnit4::class)
class AuthScreenTest {

    @Test fun emptySignInShowsFieldErrors() {
        ActivityScenario.launch(AuthActivity::class.java).use {
            onView(withId(R.id.signInButton)).perform(click())
            onView(withText(R.string.auth_error_email)).check(matches(isDisplayed()))
            onView(withText(R.string.validation_required)).check(matches(isDisplayed()))
        }
    }

    @Test fun malformedEmailIsRejected() {
        ActivityScenario.launch(AuthActivity::class.java).use {
            onView(withId(R.id.emailInput)).perform(replaceText("not-an-email"), closeSoftKeyboard())
            onView(withId(R.id.passwordInput)).perform(replaceText("whatever1"), closeSoftKeyboard())
            onView(withId(R.id.signInButton)).perform(click())
            onView(withText(R.string.auth_error_email)).check(matches(isDisplayed()))
        }
    }

    @Test fun signUpValidatesEveryField() {
        ActivityScenario.launch(AuthActivity::class.java).use {
            onView(withId(R.id.createAccountLink)).perform(click())
            onView(withId(R.id.createButton)).perform(click())
            onView(withText(R.string.auth_error_email)).check(matches(isDisplayed()))
        }
    }

    @Test fun signUpRejectsShortPassword() {
        ActivityScenario.launch(AuthActivity::class.java).use {
            onView(withId(R.id.createAccountLink)).perform(click())
            onView(withId(R.id.firstNameInput)).perform(replaceText("Jane"), closeSoftKeyboard())
            onView(withId(R.id.lastNameInput)).perform(replaceText("Doe"), closeSoftKeyboard())
            onView(withId(R.id.emailInput)).perform(replaceText("jane@example.com"), closeSoftKeyboard())
            onView(withId(R.id.passwordInput)).perform(replaceText("short"), closeSoftKeyboard())
            onView(withId(R.id.createButton)).perform(click())
            // The rule text comes from the Clerk instance's real policy (e.g. "At least 15 characters").
            onView(withId(R.id.passwordLayout)).check(matches(hasInputError(containsString("At least"))))
        }
    }

    @Test fun signInOffersForgotPassword() {
        ActivityScenario.launch(AuthActivity::class.java).use {
            onView(withId(R.id.forgotPasswordLink)).check(matches(isDisplayed()))
            onView(withId(R.id.forgotPasswordLink)).perform(click())
            onView(withText(R.string.forgot_title)).check(matches(isDisplayed()))
            onView(withId(R.id.sendButton)).check(matches(isDisplayed()))
        }
    }

    @Test fun forgotPasswordValidatesTheEmailBeforeAnyRequest() {
        ActivityScenario.launch(AuthActivity::class.java).use {
            onView(withId(R.id.forgotPasswordLink)).perform(click())
            onView(withId(R.id.emailInput)).perform(replaceText("not-an-email"), closeSoftKeyboard())
            onView(withId(R.id.sendButton)).perform(click())
            onView(withText(R.string.auth_error_email)).check(matches(isDisplayed()))
        }
    }

    @Test fun forgotPasswordCanGoBackToSignIn() {
        ActivityScenario.launch(AuthActivity::class.java).use {
            onView(withId(R.id.forgotPasswordLink)).perform(click())
            onView(withId(R.id.backToSignIn)).perform(click())
            onView(withId(R.id.signInButton)).check(matches(isDisplayed()))
        }
    }
}
