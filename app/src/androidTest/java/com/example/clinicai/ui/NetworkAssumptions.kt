package com.example.clinicai.ui

import java.net.HttpURLConnection
import java.net.URL
import org.junit.Assume

/** Tests marked "live" talk to the real deployed backend; they are skipped (not failed) when it can't be reached. */
object NetworkAssumptions {
    fun assumeBackendReachable() {
        val reachable = try {
            (URL("https://clinicai-555888.vercel.app/api/specializations").openConnection() as HttpURLConnection).run {
                connectTimeout = 5_000
                readTimeout = 8_000
                responseCode == 200
            }
        } catch (e: Exception) {
            false
        }
        Assume.assumeTrue("ClinicAI backend not reachable from the device", reachable)
    }
}
