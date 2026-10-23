package com.example.clinicai.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Display formatting. Uses SimpleDateFormat (not java.time) because the app supports API 24+.
 * Appointment dates are date-only values stored as UTC midnight, so they are formatted in UTC to
 * avoid shifting the day; "created at" timestamps are shown in the viewer's time zone.
 */
object DateFormats {

    fun longDate(isoDate: String, locale: Locale = Locale.ENGLISH): String =
        reformat(isoDate.take(10), "yyyy-MM-dd", "EEE, d MMM yyyy", locale, TimeZone.getTimeZone("UTC"), TimeZone.getTimeZone("UTC"))

    fun shortDate(isoDate: String, locale: Locale = Locale.ENGLISH): String =
        reformat(isoDate.take(10), "yyyy-MM-dd", "d MMM yyyy", locale, TimeZone.getTimeZone("UTC"), TimeZone.getTimeZone("UTC"))

    /** `17:00` -> `5:00 PM`. Returns the input unchanged if it isn't `HH:mm`. */
    fun time12h(time: String): String {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return time
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return time
        val period = if (hour >= 12) "PM" else "AM"
        val h12 = if (hour % 12 == 0) 12 else hour % 12
        return "%d:%02d %s".format(Locale.ENGLISH, h12, minute, period)
    }

    /** ISO instant such as `2026-10-23T10:15:00.000Z` -> `23 Oct 2026, 3:45 PM` in [zone]. */
    fun timestamp(iso: String?, zone: TimeZone = TimeZone.getDefault(), locale: Locale = Locale.ENGLISH): String? {
        if (iso.isNullOrBlank()) return null
        val parsed = parseInstant(iso) ?: return null
        return SimpleDateFormat("d MMM yyyy, h:mm a", locale).apply { timeZone = zone }.format(parsed)
    }

    private fun parseInstant(iso: String): java.util.Date? {
        val trimmed = iso.substringBefore("Z").substringBefore("+").let {
            // drop fractional seconds
            if (it.contains('.')) it.substringBefore('.') else it
        }
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(trimmed)
        } catch (e: ParseException) {
            null
        }
    }

    private fun reformat(
        value: String,
        from: String,
        to: String,
        locale: Locale,
        fromZone: TimeZone,
        toZone: TimeZone,
    ): String = try {
        val date = SimpleDateFormat(from, Locale.US).apply { timeZone = fromZone }.parse(value)
        if (date == null) value else SimpleDateFormat(to, locale).apply { timeZone = toZone }.format(date)
    } catch (e: ParseException) {
        value
    }

    /** Indian digit grouping: 1200 -> `1,200`, 100000 -> `1,00,000`. */
    fun rupees(amount: Int): String {
        val digits = Math.abs(amount.toLong()).toString()
        val grouped = if (digits.length <= 3) {
            digits
        } else {
            // Last three digits, then groups of two: 1234567 -> 12,34,567
            val head = digits.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
            "$head,${digits.takeLast(3)}"
        }
        return if (amount < 0) "-$grouped" else grouped
    }

    /** Paise -> rupee string with two decimals only when needed: 10000 -> `100`, 5050 -> `50.50`. */
    fun rupeesFromPaise(paise: Int): String {
        val whole = paise / 100
        val rem = paise % 100
        return if (rem == 0) rupees(whole) else "${rupees(whole)}.${rem.toString().padStart(2, '0')}"
    }
}
