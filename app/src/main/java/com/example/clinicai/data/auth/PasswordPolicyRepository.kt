package com.example.clinicai.data.auth

import android.util.Base64
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Reads the password rules from Clerk's public environment endpoint (no secrets, same data the
 * website's sign-up form uses). Cached for the process lifetime; falls back to a conservative
 * default if the request fails, because Clerk re-checks every password on the server anyway.
 */
class PasswordPolicyRepository(
    private val publishableKey: String,
    private val client: OkHttpClient = OkHttpClient(),
) {
    private val mutex = Mutex()
    @Volatile private var cached: PasswordPolicy? = null

    suspend fun policy(): PasswordPolicy {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: withContext(Dispatchers.IO) { fetch() }.also { if (it != null) cached = it } ?: PasswordPolicy()
        }
    }

    private fun fetch(): PasswordPolicy? = try {
        val host = frontendApiHost(publishableKey) ?: return null
        val request = Request.Builder().url("https://$host/v1/environment").header("Accept", "application/json").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            parse(response.body?.string().orEmpty())
        }
    } catch (e: IOException) {
        null
    } catch (e: RuntimeException) {
        null
    }

    companion object {
        /** `pk_test_<base64(host$)>` -> host. */
        fun frontendApiHost(publishableKey: String): String? {
            val encoded = publishableKey.split("_").getOrNull(2) ?: return null
            return try {
                String(Base64.decode(encoded, Base64.DEFAULT)).trimEnd('$').takeIf { it.isNotBlank() }
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        fun parse(json: String): PasswordPolicy? {
            val settings = (JsonParser.parseString(json) as? JsonObject)
                ?.getAsJsonObject("user_settings")?.getAsJsonObject("password_settings") ?: return null
            fun bool(name: String) = settings.get(name)?.takeIf { !it.isJsonNull }?.asBoolean ?: false
            fun int(name: String) = settings.get(name)?.takeIf { !it.isJsonNull }?.asInt ?: 0
            return PasswordPolicy(
                minLength = int("min_length").takeIf { it > 0 } ?: PasswordPolicy.DEFAULT_MIN,
                maxLength = int("max_length"),
                requireUppercase = bool("require_uppercase"),
                requireLowercase = bool("require_lowercase"),
                requireNumbers = bool("require_numbers"),
                requireSpecialChar = bool("require_special_char"),
            )
        }
    }
}
