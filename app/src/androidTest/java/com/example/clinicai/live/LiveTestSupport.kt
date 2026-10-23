package com.example.clinicai.live

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.example.clinicai.AppContainer
import com.example.clinicai.ClinicAIApplication
import java.util.UUID
import org.junit.Assume

/**
 * Support for the opt-in "live" tests. They talk to the REAL Clerk instance (creating throw-away
 * users with Clerk's documented `+clerk_test` addresses, whose verification code is always 424242
 * and which never send e-mail) and to whatever backend the debug build points at.
 *
 * Run them with `-e liveAuth true`, e.g. through scripts/run-live-tests.sh. Without the flag they
 * are skipped, so a normal `connectedDebugAndroidTest` never creates users.
 */
object LiveTestSupport {
    const val TEST_CODE = "424242"

    val args: Bundle get() = InstrumentationRegistry.getArguments()

    fun assumeLive() = Assume.assumeTrue("live tests are opt-in: pass -e liveAuth true", args.getString("liveAuth") == "true")

    fun arg(name: String): String = args.getString(name) ?: error("missing instrumentation argument -e $name")

    val container: AppContainer
        get() = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ClinicAIApplication).container

    /** A fresh Clerk test address: never delivers real mail, always accepts [TEST_CODE]. */
    fun newEmail(tag: String) = "qa.$tag.${UUID.randomUUID().toString().take(8)}+clerk_test@example.com"

    /** Long enough for the instance's 15-character minimum and not a known breached password. */
    fun newPassword() = "Clinic-QA-${UUID.randomUUID().toString().take(10)}-Zq"

    /** Retries [assertion] until it passes or [timeoutMs] elapses (network calls make timing variable). */
    fun waitFor(timeoutMs: Long = 25_000, assertion: () -> Unit) {
        val end = System.currentTimeMillis() + timeoutMs
        var last: Throwable? = null
        while (System.currentTimeMillis() < end) {
            try {
                assertion()
                return
            } catch (e: Throwable) {
                last = e
                Thread.sleep(300)
            }
        }
        throw last ?: AssertionError("timed out after $timeoutMs ms")
    }
}
