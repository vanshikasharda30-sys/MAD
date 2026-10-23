package com.example.clinicai.live

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clinicai.data.auth.AuthOutcome
import com.example.clinicai.data.auth.AuthState
import com.example.clinicai.live.LiveTestSupport.TEST_CODE
import com.example.clinicai.live.LiveTestSupport.assumeLive
import com.example.clinicai.live.LiveTestSupport.container
import com.example.clinicai.live.LiveTestSupport.newEmail
import com.example.clinicai.live.LiveTestSupport.newPassword
import com.example.clinicai.live.LiveTestSupport.waitFor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Not a regression test: it leaves a real signed-in session on the device so a person (or adb) can
 * kill the app and watch how a *restored* session behaves on a cold start. Run it explicitly.
 */
@RunWith(AndroidJUnit4::class)
class LiveSessionFixture {
    @Test fun registerAndStaySignedIn() {
        assumeLive()
        runBlocking {
            container.auth.signOut()
            check(container.auth.signUp("Restore", "Tester", newEmail("restore"), newPassword()) is AuthOutcome.NeedsCode)
            check(container.auth.submitCode(TEST_CODE) is AuthOutcome.SignedIn)
        }
        waitFor { assertTrue(container.auth.authState.value is AuthState.SignedIn) }
    }
}
