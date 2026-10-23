package com.example.clinicai.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CheckinTokenParserTest {
    @Test fun extractsTokenFromWebsiteLink() {
        assertEquals("abc123", CheckinTokenParser.parse("https://clinicai-555888.vercel.app/checkin/abc123"))
    }

    @Test fun ignoresQueryAndTrailingSlash() {
        assertEquals("abc123", CheckinTokenParser.parse("https://x.app/checkin/abc123/?src=qr"))
    }

    @Test fun acceptsBareCode() {
        assertEquals("abc123", CheckinTokenParser.parse("  abc123 "))
    }

    @Test fun rejectsEmptyAndPathLikeValues() {
        assertNull(CheckinTokenParser.parse(null))
        assertNull(CheckinTokenParser.parse("  "))
        assertNull(CheckinTokenParser.parse("https://x.app/other/path"))
    }
}
