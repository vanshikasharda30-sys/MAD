package com.example.clinicai.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrivacyMaskerTest {
    @Test fun masksEmailKeepingFirstLetterAndDomain() {
        assertEquals("j***@gmail.com", PrivacyMasker.maskEmail("jane.doe@gmail.com"))
    }

    @Test fun masksPhoneKeepingLastFour() {
        assertEquals("********3210", PrivacyMasker.maskPhone("+91 98765 43210"))
    }

    @Test fun nullAndBlankStayNull() {
        assertNull(PrivacyMasker.maskEmail(null))
        assertNull(PrivacyMasker.maskEmail(" "))
        assertNull(PrivacyMasker.maskPhone(""))
    }

    @Test fun shortNumbersAreNotPadded() {
        assertEquals("1234", PrivacyMasker.maskPhone("1234"))
    }
}
