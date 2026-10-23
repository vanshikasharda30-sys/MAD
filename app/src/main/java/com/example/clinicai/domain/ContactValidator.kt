package com.example.clinicai.domain

/**
 * Decides whether a stored clinic phone number / e-mail is something a patient can actually use.
 *
 * The database currently holds placeholder contact data (for example `info.demo@…​.example`, and a
 * `+91-22-9999-0000` number). Showing those as real contact details — or offering a "Call" /
 * "Email" button for them — is misleading, so anything that fails these checks is treated as
 * "not available". Nothing is ever guessed or invented.
 */
object ContactValidator {

    data class Phone(
        /** What to show, e.g. `+91 22 2445 2575`. */
        val display: String,
        /** What to dial, e.g. `+912224452575`. */
        val dial: String,
    )

    private val EMAIL = Regex("^[A-Za-z0-9._%+\\-]+@([A-Za-z0-9\\-]+\\.)+[A-Za-z]{2,}$")

    /** Reserved by RFC 2606 / 6761: these can never receive mail. */
    private val RESERVED_TLDS = setOf("example", "invalid", "test", "localhost", "local")
    private val RESERVED_DOMAINS = setOf("example.com", "example.org", "example.net")

    fun email(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty() || !EMAIL.matches(value) || value.contains("..")) return null
        val domain = value.substringAfter('@').lowercase()
        if (domain.substringAfterLast('.') in RESERVED_TLDS || domain in RESERVED_DOMAINS) return null
        return value
    }

    /**
     * Accepts international (`+91 …`) or national numbers with 8–15 digits (E.164 allows 15).
     * Rejects obvious placeholders: all digits identical, or a run of four identical digits in
     * the subscriber part (`9999`, `0000`), which real numbers practically never contain.
     */
    fun phone(raw: String?): Phone? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        if (text.any { !(it.isDigit() || it in "+-() .") }) return null // letters etc.
        if (text.count { it == '+' } > 1 || (text.contains('+') && !text.startsWith("+"))) return null

        val digits = text.filter { it.isDigit() }
        if (digits.length !in 8..15) return null
        if (digits.toSet().size == 1) return null
        if (Regex("(\\d)\\1{3,}").containsMatchIn(digits.takeLast(8))) return null

        val international = text.startsWith("+")
        val dial = if (international) "+$digits" else digits
        return Phone(display = prettify(digits, international), dial = dial)
    }

    /** `+912224452575` -> `+91 22 2445 2575` for Indian numbers; otherwise grouped plainly. */
    private fun prettify(digits: String, international: Boolean): String {
        if (international && digits.startsWith("91") && digits.length == 12) {
            val national = digits.drop(2)
            return "+91 ${national.take(2)} ${national.substring(2, 6)} ${national.substring(6)}"
        }
        return (if (international) "+" else "") + digits.chunked(4).joinToString(" ")
    }
}
