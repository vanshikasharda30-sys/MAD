package com.example.clinicai.live

import android.app.Activity
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clinicai.R
import com.example.clinicai.data.auth.AuthState
import com.example.clinicai.data.remote.ApiResult
import com.example.clinicai.live.LiveTestSupport.TEST_CODE
import com.example.clinicai.live.LiveTestSupport.arg
import com.example.clinicai.live.LiveTestSupport.assumeLive
import com.example.clinicai.live.LiveTestSupport.container
import com.example.clinicai.live.LiveTestSupport.newEmail
import com.example.clinicai.live.LiveTestSupport.newPassword
import com.example.clinicai.live.LiveTestSupport.waitFor
import com.example.clinicai.testing.hasInputError
import com.example.clinicai.ui.auth.AuthActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Registration, the additional sign-in challenge from the bug report, and password recovery —
 * driven through the real screens against the real Clerk instance.
 */
@RunWith(AndroidJUnit4::class)
class LiveAccountFlowTest {

    @Before fun setUp() {
        assumeLive()
        runBlocking { container.auth.signOut() }
        waitFor { assertTrue(container.auth.authState.value is AuthState.SignedOut) }
    }

    @After fun tearDown() {
        if (args.getString("liveAuth") == "true") runBlocking { container.auth.signOut() }
    }

    private val args get() = LiveTestSupport.args

    /** The activity's result code, failing (so waitFor retries) while it hasn't finished yet. */
    private fun finishedResult(scenario: ActivityScenario<*>): Int {
        if (scenario.state != androidx.lifecycle.Lifecycle.State.DESTROYED) throw AssertionError("AuthActivity has not finished yet")
        return scenario.result.resultCode
    }

    private fun type(id: Int, text: String) = onView(withId(id)).perform(replaceText(text), closeSoftKeyboard())

    private fun typeCodeAndVerify(code: String) {
        waitFor { onView(withId(R.id.codeInput)).check(matches(isDisplayed())) }
        type(R.id.codeInput, code)
        onView(withId(R.id.verifyButton)).perform(click())
    }

    private fun assertSignedInAsPatient(email: String) {
        waitFor { assertTrue(container.auth.authState.value is AuthState.SignedIn) }
        val me = runBlocking { container.users.me() }
        assertTrue("backend must recognise the new account: $me", me is ApiResult.Success)
        val profile = (me as ApiResult.Success).data
        assertEquals("PATIENT", profile.type)
        assertEquals(email.lowercase(), profile.email)
    }

    // ---- Registration -------------------------------------------------------------------------

    @Test fun newAccountCanRegisterVerifyEmailAndGetAPatientRecord() {
        val email = newEmail("signup")
        ActivityScenario.launchActivityForResult(AuthActivity::class.java).use { scenario ->
            onView(withId(R.id.createAccountLink)).perform(click())
            type(R.id.firstNameInput, "Asha")
            type(R.id.lastNameInput, "Tester")
            type(R.id.emailInput, email)
            type(R.id.passwordInput, newPassword())
            onView(withId(R.id.createButton)).perform(click())

            waitFor { onView(withText(R.string.verify_title_signup)).check(matches(isDisplayed())) }
            typeCodeAndVerify(TEST_CODE)

            waitFor { assertEquals(Activity.RESULT_OK, finishedResult(scenario)) }
        }
        assertSignedInAsPatient(email)
    }

    @Test fun wrongVerificationCodeIsRejectedAndTheCorrectOneStillWorks() {
        val email = newEmail("badcode")
        ActivityScenario.launchActivityForResult(AuthActivity::class.java).use { scenario ->
            onView(withId(R.id.createAccountLink)).perform(click())
            type(R.id.firstNameInput, "Asha"); type(R.id.lastNameInput, "Tester")
            type(R.id.emailInput, email); type(R.id.passwordInput, newPassword())
            onView(withId(R.id.createButton)).perform(click())

            typeCodeAndVerify("000000")
            waitFor { onView(withText(R.string.auth_error_code_invalid)).check(matches(isDisplayed())) }
            assertTrue("a wrong code must not sign anyone in", container.auth.authState.value !is AuthState.SignedIn)

            type(R.id.codeInput, TEST_CODE)
            onView(withId(R.id.verifyButton)).perform(click())
            waitFor { assertEquals(Activity.RESULT_OK, finishedResult(scenario)) }
        }
        assertSignedInAsPatient(email)
    }

    @Test fun signUpEnforcesTheInstancesRealPasswordPolicy() {
        ActivityScenario.launchActivityForResult(AuthActivity::class.java).use {
            onView(withId(R.id.createAccountLink)).perform(click())
            // The Clerk instance requires 15+ characters; the screen must read that, not assume 8.
            waitFor { onView(withId(R.id.passwordLayout)).check(matches(hasHelper(containsString("15")))) }
            type(R.id.firstNameInput, "Asha"); type(R.id.lastNameInput, "Tester")
            type(R.id.emailInput, newEmail("shortpw")); type(R.id.passwordInput, "Abcd1234efgh")
            onView(withId(R.id.createButton)).perform(click())
            onView(withId(R.id.passwordLayout)).check(matches(hasInputError(containsString("15"))))
        }
    }

    @Test fun registeringAnExistingEmailExplainsWhatToDo() {
        val existing = arg("qaEmail")
        ActivityScenario.launchActivityForResult(AuthActivity::class.java).use {
            onView(withId(R.id.createAccountLink)).perform(click())
            type(R.id.firstNameInput, "Asha"); type(R.id.lastNameInput, "Tester")
            type(R.id.emailInput, existing); type(R.id.passwordInput, newPassword())
            onView(withId(R.id.createButton)).perform(click())
            waitFor { onView(withText(R.string.auth_error_account_exists)).check(matches(isDisplayed())) }
        }
    }

    // ---- The reported sign-in problem ------------------------------------------------------------

    /**
     * The bug report: signing in with a correct password said "Your account needs an additional step
     * that the app can't complete yet." Clerk's actual state was `needs_client_trust` (new-device
     * e-mail code). The account here was created by a different client, exactly like a returning user.
     */
    @Test fun existingAccountOnANewDeviceCompletesTheEmailCodeChallengeAndSignsIn() {
        val email = arg("qaEmail"); val password = arg("qaPassword")
        ActivityScenario.launchActivityForResult(AuthActivity::class.java).use { scenario ->
            type(R.id.emailInput, email); type(R.id.passwordInput, password)
            onView(withId(R.id.signInButton)).perform(click())

            waitFor { onView(withText(R.string.verify_title_second_factor)).check(matches(isDisplayed())) }
            onView(withText(containsString("new device"))).check(matches(isDisplayed()))
            assertTrue("no session may exist before the challenge is completed", container.auth.authState.value !is AuthState.SignedIn)

            typeCodeAndVerify("000000")
            waitFor { onView(withText(R.string.auth_error_code_invalid)).check(matches(isDisplayed())) }

            type(R.id.codeInput, TEST_CODE)
            onView(withId(R.id.verifyButton)).perform(click())
            waitFor { assertEquals(Activity.RESULT_OK, finishedResult(scenario)) }
        }
        assertSignedInAsPatient(email)
    }

    @Test fun wrongPasswordGivesAClearMessageAndNoSession() {
        val email = arg("qaEmail")
        ActivityScenario.launchActivityForResult(AuthActivity::class.java).use {
            type(R.id.emailInput, email); type(R.id.passwordInput, "definitely-the-wrong-password-1")
            onView(withId(R.id.signInButton)).perform(click())
            waitFor { onView(withText(R.string.auth_error_invalid_credentials)).check(matches(isDisplayed())) }
        }
        assertTrue(container.auth.authState.value !is AuthState.SignedIn)
    }

    @Test fun alreadySignedInUsersAreNeverAskedToSignInAgain() {
        val email = arg("qaEmail"); val password = arg("qaPassword")
        // Sign in once through the manager (covering the challenge), then open the sign-in screen again.
        runBlocking {
            val first = container.auth.signIn(email, password)
            if (first is com.example.clinicai.data.auth.AuthOutcome.NeedsCode) container.auth.submitCode(TEST_CODE)
        }
        waitFor { assertTrue(container.auth.authState.value is AuthState.SignedIn) }
        ActivityScenario.launchActivityForResult(AuthActivity::class.java).use { scenario ->
            waitFor(10_000) { assertEquals(Activity.RESULT_OK, finishedResult(scenario)) }
        }
    }

    // ---- Forgot password ----------------------------------------------------------------------

    @Test fun forgotPasswordResetsThePasswordWithAnEmailedCode() {
        val email = arg("resetEmail"); val newPw = arg("resetNewPassword")
        ActivityScenario.launchActivityForResult(AuthActivity::class.java).use { scenario ->
            onView(withId(R.id.forgotPasswordLink)).perform(click())
            type(R.id.emailInput, email)
            onView(withId(R.id.sendButton)).perform(click())

            waitFor { onView(withText(R.string.verify_title_reset)).check(matches(isDisplayed())) }
            // The wording is neutral: it never claims the address is registered.
            onView(withText(containsString("If an account exists"))).check(matches(isDisplayed()))
            // Resend is rate-limited by a visible cooldown.
            onView(withId(R.id.resendButton)).check(matches(withText(containsString("Resend code in"))))

            typeCodeAndVerify("111111")
            waitFor { onView(withText(R.string.auth_error_code_invalid)).check(matches(isDisplayed())) }
            type(R.id.codeInput, TEST_CODE)
            onView(withId(R.id.verifyButton)).perform(click())

            waitFor { onView(withText(R.string.new_password_title)).check(matches(isDisplayed())) }
            // Validation: mismatch, then a password below the policy.
            type(R.id.passwordInput, newPw); type(R.id.confirmInput, newPw + "x")
            onView(withId(R.id.updateButton)).perform(click())
            onView(withId(R.id.confirmLayout)).check(matches(hasInputError(containsString("match"))))
            type(R.id.passwordInput, "short"); type(R.id.confirmInput, "short")
            onView(withId(R.id.updateButton)).perform(click())
            onView(withId(R.id.passwordLayout)).check(matches(hasInputError(containsString("At least"))))
            assertTrue("nothing may be signed in mid-reset", container.auth.authState.value !is AuthState.SignedIn)

            type(R.id.passwordInput, newPw); type(R.id.confirmInput, newPw)
            onView(withId(R.id.updateButton)).perform(click())

            // Success is reported only after Clerk confirmed the change, and the user must sign in again.
            waitFor { onView(withText(R.string.auth_password_updated)).check(matches(isDisplayed())) }
            assertTrue("the user must sign in again with the new password", container.auth.authState.value !is AuthState.SignedIn)

            type(R.id.emailInput, email); type(R.id.passwordInput, newPw)
            onView(withId(R.id.signInButton)).perform(click())
            // Clerk may ask for the new-device code again; complete it if so.
            waitFor(30_000) {
                if (runCatching { onView(withId(R.id.codeInput)).check(matches(isDisplayed())) }.isSuccess) {
                    type(R.id.codeInput, TEST_CODE)
                    onView(withId(R.id.verifyButton)).perform(click())
                }
                assertEquals(Activity.RESULT_OK, finishedResult(scenario))
            }
        }
        waitFor { assertTrue(container.auth.authState.value is AuthState.SignedIn) }
    }

    @Test fun forgotPasswordForAnUnknownEmailRevealsNothing() {
        ActivityScenario.launchActivityForResult(AuthActivity::class.java).use {
            onView(withId(R.id.forgotPasswordLink)).perform(click())
            type(R.id.emailInput, newEmail("nobody"))
            onView(withId(R.id.sendButton)).perform(click())
            // Same screen and wording as for a real account.
            waitFor { onView(withText(R.string.verify_title_reset)).check(matches(isDisplayed())) }
            onView(withText(containsString("If an account exists"))).check(matches(isDisplayed()))
            typeCodeAndVerify(TEST_CODE)
            // ...and no code can ever succeed for an address that has no account.
            waitFor { onView(withText(R.string.auth_error_code_invalid)).check(matches(isDisplayed())) }
        }
        assertNotNull(container.auth)
    }

    private fun hasHelper(matcher: org.hamcrest.Matcher<String>): org.hamcrest.Matcher<android.view.View> =
        object : androidx.test.espresso.matcher.BoundedMatcher<android.view.View, com.google.android.material.textfield.TextInputLayout>(
            com.google.android.material.textfield.TextInputLayout::class.java,
        ) {
            override fun describeTo(description: org.hamcrest.Description) { description.appendText("helper text ").appendDescriptionOf(matcher) }
            override fun matchesSafely(view: com.google.android.material.textfield.TextInputLayout) =
                view.helperText?.toString()?.let { matcher.matches(it) } ?: false
        }
}
