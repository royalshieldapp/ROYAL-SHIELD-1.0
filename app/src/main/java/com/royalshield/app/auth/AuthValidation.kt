package com.royalshield.app.auth

private val EMAIL_PATTERN =
    Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
private val E164_PATTERN = Regex("^\\+[1-9]\\d{7,14}$")

internal fun normalizeEmail(value: String): String = value.trim().lowercase()

internal fun isValidEmail(value: String): Boolean =
    EMAIL_PATTERN.matches(normalizeEmail(value))

internal fun passwordValidationMessage(value: String): String? = when {
    value.length < 8 -> "Password must contain at least 8 characters"
    value.none(Char::isUpperCase) -> "Password must contain an uppercase letter"
    value.none(Char::isLowerCase) -> "Password must contain a lowercase letter"
    value.none(Char::isDigit) -> "Password must contain a number"
    else -> null
}

internal fun normalizeE164Phone(value: String): String =
    value.trim().replace(Regex("[\\s()\\-]"), "")

internal fun isValidE164Phone(value: String): Boolean =
    E164_PATTERN.matches(normalizeE164Phone(value))

internal fun isValidOtp(value: String): Boolean =
    value.length == 6 && value.all(Char::isDigit)

internal fun maskPhone(value: String): String {
    val normalized = normalizeE164Phone(value)
    if (normalized.length <= 4) return normalized
    return "${normalized.take(2)}••••••${normalized.takeLast(4)}"
}
