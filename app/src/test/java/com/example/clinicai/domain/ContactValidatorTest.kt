package com.example.clinicai.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContactValidatorTest {

    @Test fun everyPlaceholderEmailInTheDatabaseIsRejected() {
        // Verbatim from the production clinics table: all use the reserved ".example" TLD.
        listOf(
            "info.demo@lilavatihospital.example", "info.demo@kokilabenhospital.example",
            "info.demo@nanavatimax.example", "info.demo@hiranandanihospital.example",
            "info.demo@rahejahospital.example", "info.demo@hindujahospital.example",
            "info.demo@globalhospitalmumbai.example", "demo.clinicai.example",
        ).forEach { assertNull(it, ContactValidator.email(it)) }
    }

    @Test fun reservedTestDomainsAreRejected() {
        listOf("a@b.invalid", "a@b.test", "a@host.localhost", "a@example.com", "a@example.org", "a@mail.local")
            .forEach { assertNull(it, ContactValidator.email(it)) }
    }

    @Test fun realLookingEmailsPass() {
        assertEquals("reception@lilavatihospital.com", ContactValidator.email("  reception@lilavatihospital.com "))
        assertNotNull(ContactValidator.email("info+appointments@hindujahospital.in"))
    }

    @Test fun malformedEmailsAreRejected() {
        listOf(null, "", "plain", "a@b", "@b.com", "a b@c.com", "a@@b.com", "a..b@c.com", "a@b..com")
            .forEach { assertNull(it, ContactValidator.email(it)) }
    }

    @Test fun theDemoPhoneInTheDatabaseIsRejected() {
        assertNull("+91-22-9999-0000 is a placeholder", ContactValidator.phone("+91-22-9999-0000"))
    }

    @Test fun realIndianLandlinesPassAndAreFormatted() {
        val phone = ContactValidator.phone("+91-22-2445-2575")!!
        assertEquals("+91 22 2445 2575", phone.display)
        assertEquals("+912224452575", phone.dial)
        assertNotNull(ContactValidator.phone("+91-22-4269-6969"))
        assertNotNull(ContactValidator.phone("+91-22-6767-6767"))
    }

    @Test fun mobileAndNationalFormatsPass() {
        assertEquals("+919876543210", ContactValidator.phone("+91 98765 43210")!!.dial)
        assertEquals("02224452575", ContactValidator.phone("022 2445 2575")!!.dial)
    }

    @Test fun obviousPlaceholdersAndGarbageAreRejected() {
        listOf(null, "", "   ", "12345", "+1 (000) 000-0000", "1111111111", "abc-defg-hijk", "+91-+22-1234-5678", "12345678901234567")
            .forEach { assertNull(it, ContactValidator.phone(it)) }
    }
}
