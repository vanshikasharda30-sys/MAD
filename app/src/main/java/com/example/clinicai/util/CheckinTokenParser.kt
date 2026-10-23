package com.example.clinicai.util

import com.example.clinicai.config.AppConstants

/** Extracts the clinic QR token from a scanned QR payload, a pasted link or a bare code. */
object CheckinTokenParser {

    fun parse(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null

        val marker = "/${AppConstants.CHECKIN_PATH}/"
        val token = if (text.contains(marker, ignoreCase = true)) {
            text.substring(text.indexOf(marker, ignoreCase = true) + marker.length)
        } else {
            text
        }
        return token.substringBefore("?").substringBefore("#").trim('/', ' ').takeIf { it.isNotEmpty() && !it.contains('/') }
    }
}
