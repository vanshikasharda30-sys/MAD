package com.example.clinicai.data.auth

/** The password rules configured on the Clerk instance (the app reads them instead of guessing). */
data class PasswordPolicy(
    val minLength: Int = DEFAULT_MIN,
    val maxLength: Int = 0,
    val requireUppercase: Boolean = false,
    val requireLowercase: Boolean = false,
    val requireNumbers: Boolean = false,
    val requireSpecialChar: Boolean = false,
) {
    enum class Rule { MIN_LENGTH, MAX_LENGTH, UPPERCASE, LOWERCASE, NUMBER, SPECIAL }

    /** The rules [password] does not satisfy; empty when it is acceptable on the client side. */
    fun violations(password: String): List<Rule> = buildList {
        if (password.length < minLength) add(Rule.MIN_LENGTH)
        if (maxLength > 0 && password.length > maxLength) add(Rule.MAX_LENGTH)
        if (requireUppercase && password.none { it.isUpperCase() }) add(Rule.UPPERCASE)
        if (requireLowercase && password.none { it.isLowerCase() }) add(Rule.LOWERCASE)
        if (requireNumbers && password.none { it.isDigit() }) add(Rule.NUMBER)
        if (requireSpecialChar && password.all { it.isLetterOrDigit() }) add(Rule.SPECIAL)
    }

    fun isAcceptable(password: String) = violations(password).isEmpty()

    /** Rules to show as hints, in reading order. */
    val hints: List<Pair<Rule, Int>>
        get() = buildList {
            add(Rule.MIN_LENGTH to minLength)
            if (requireUppercase) add(Rule.UPPERCASE to 0)
            if (requireLowercase) add(Rule.LOWERCASE to 0)
            if (requireNumbers) add(Rule.NUMBER to 0)
            if (requireSpecialChar) add(Rule.SPECIAL to 0)
        }

    companion object {
        /** Used until (or unless) the real policy can be fetched; Clerk still enforces its own rules server-side. */
        const val DEFAULT_MIN = 8
    }
}
