package com.example.clinicai.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordPolicyTest {

    @Test fun parsesTheRealClerkEnvironment() {
        // Trimmed from https://pumped-eft-15.clerk.accounts.dev/v1/environment
        val json = """{"user_settings":{"password_settings":{"disable_hibp":false,"min_length":15,"max_length":0,
            "require_special_char":false,"require_numbers":false,"require_uppercase":false,"require_lowercase":false}}}"""
        val policy = PasswordPolicyRepository.parse(json)!!
        assertEquals(15, policy.minLength)
        assertFalse(policy.requireNumbers)
    }

    @Test fun parsesStrictRules() {
        val json = """{"user_settings":{"password_settings":{"min_length":10,"max_length":64,"require_special_char":true,
            "require_numbers":true,"require_uppercase":true,"require_lowercase":true}}}"""
        val policy = PasswordPolicyRepository.parse(json)!!
        // "x" is lowercase, so every rule except the lowercase one is broken.
        assertEquals(listOf(PasswordPolicy.Rule.MIN_LENGTH, PasswordPolicy.Rule.UPPERCASE, PasswordPolicy.Rule.NUMBER, PasswordPolicy.Rule.SPECIAL),
            policy.violations("x"))
        assertTrue(policy.isAcceptable("Str0ng!Passw0rd"))
    }

    @Test fun missingOrBrokenSettingsFallBackToTheDefault() {
        assertNull(PasswordPolicyRepository.parse("""{"user_settings":{}}"""))
        assertEquals(PasswordPolicy.DEFAULT_MIN, PasswordPolicyRepository.parse("""{"user_settings":{"password_settings":{"min_length":0}}}""")!!.minLength)
    }

    @Test fun lengthRuleMatchesWhatClerkEnforces() {
        val policy = PasswordPolicy(minLength = 15)
        assertFalse(policy.isAcceptable("Abcd1234")) // the 8-character password the app used to allow
        assertFalse(policy.isAcceptable("a".repeat(14)))
        assertTrue(policy.isAcceptable("a".repeat(15)))
    }

    @Test fun maxLengthWhenConfigured() {
        val policy = PasswordPolicy(minLength = 8, maxLength = 12)
        assertEquals(listOf(PasswordPolicy.Rule.MAX_LENGTH), policy.violations("x".repeat(13)))
    }

    @Test fun hintsListOnlyActiveRules() {
        assertEquals(listOf(PasswordPolicy.Rule.MIN_LENGTH), PasswordPolicy(minLength = 15).hints.map { it.first })
    }
}
