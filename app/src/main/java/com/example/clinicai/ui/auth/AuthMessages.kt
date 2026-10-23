package com.example.clinicai.ui.auth

import android.content.Context
import com.example.clinicai.R
import com.example.clinicai.data.auth.AuthFailureKind
import com.example.clinicai.data.auth.AuthOutcome
import com.example.clinicai.data.auth.CodeChallenge
import com.example.clinicai.data.auth.CodePurpose
import com.example.clinicai.data.auth.PasswordPolicy

/** Text for a failed auth step. Password-rule failures use Clerk's own wording because it names the exact rule. */
fun AuthOutcome.toMessage(context: Context): String = when (this) {
    is AuthOutcome.Failed -> when (kind) {
        AuthFailureKind.NETWORK -> context.getString(R.string.error_offline)
        AuthFailureKind.NATIVE_API_DISABLED -> context.getString(R.string.auth_native_disabled)
        AuthFailureKind.INVALID_CREDENTIALS -> context.getString(R.string.auth_error_invalid_credentials)
        AuthFailureKind.CODE_INVALID -> context.getString(R.string.auth_error_code_invalid)
        AuthFailureKind.CODE_EXPIRED -> context.getString(R.string.auth_error_code_expired)
        AuthFailureKind.TOO_MANY_ATTEMPTS -> context.getString(R.string.auth_error_too_many_attempts)
        AuthFailureKind.RATE_LIMITED -> context.getString(R.string.auth_error_rate_limited)
        AuthFailureKind.ACCOUNT_EXISTS -> context.getString(R.string.auth_error_account_exists)
        AuthFailureKind.FLOW_EXPIRED -> context.getString(R.string.auth_error_flow_expired)
        AuthFailureKind.PASSWORD_REJECTED, AuthFailureKind.GENERIC -> message ?: context.getString(R.string.error_generic)
    }
    is AuthOutcome.Unsupported -> context.getString(R.string.auth_error_unsupported, status)
    else -> context.getString(R.string.error_generic)
}

fun CodeChallenge.title(context: Context): String = context.getString(
    when (purpose) {
        CodePurpose.SIGN_UP_EMAIL -> R.string.verify_title_signup
        CodePurpose.SIGN_IN_EMAIL -> R.string.verify_title_signin
        CodePurpose.SECOND_FACTOR_EMAIL, CodePurpose.SECOND_FACTOR_PHONE -> R.string.verify_title_second_factor
        CodePurpose.SECOND_FACTOR_TOTP -> R.string.verify_title_totp
        CodePurpose.PASSWORD_RESET -> R.string.verify_title_reset
    },
)

fun CodeChallenge.body(context: Context): String {
    val where = destination.orEmpty()
    return context.getString(
        when (purpose) {
            CodePurpose.SIGN_UP_EMAIL -> R.string.verify_body_signup
            CodePurpose.SIGN_IN_EMAIL -> R.string.verify_body_signin
            CodePurpose.SECOND_FACTOR_EMAIL -> R.string.verify_body_second_factor_email
            CodePurpose.SECOND_FACTOR_PHONE -> R.string.verify_body_second_factor_phone
            CodePurpose.SECOND_FACTOR_TOTP -> R.string.verify_body_totp
            CodePurpose.PASSWORD_RESET -> R.string.verify_body_reset
        },
        where,
    )
}

/** "At least 15 characters · A number …" for the password hint line. */
fun PasswordPolicy.hintText(context: Context): String = hints.joinToString(" · ") { (rule, n) ->
    when (rule) {
        PasswordPolicy.Rule.MIN_LENGTH -> context.getString(R.string.password_rule_length, n)
        PasswordPolicy.Rule.UPPERCASE -> context.getString(R.string.password_rule_upper)
        PasswordPolicy.Rule.LOWERCASE -> context.getString(R.string.password_rule_lower)
        PasswordPolicy.Rule.NUMBER -> context.getString(R.string.password_rule_number)
        PasswordPolicy.Rule.SPECIAL -> context.getString(R.string.password_rule_special)
        PasswordPolicy.Rule.MAX_LENGTH -> context.getString(R.string.password_rule_max, maxLength)
    }
}

/** The first unmet rule, phrased for an inline field error, or null if the password is fine. */
fun PasswordPolicy.errorFor(context: Context, password: String): String? {
    val first = violations(password).firstOrNull() ?: return null
    return when (first) {
        PasswordPolicy.Rule.MIN_LENGTH -> context.getString(R.string.password_rule_length, minLength)
        PasswordPolicy.Rule.MAX_LENGTH -> context.getString(R.string.password_rule_max, maxLength)
        PasswordPolicy.Rule.UPPERCASE -> context.getString(R.string.password_rule_upper)
        PasswordPolicy.Rule.LOWERCASE -> context.getString(R.string.password_rule_lower)
        PasswordPolicy.Rule.NUMBER -> context.getString(R.string.password_rule_number)
        PasswordPolicy.Rule.SPECIAL -> context.getString(R.string.password_rule_special)
    }
}
