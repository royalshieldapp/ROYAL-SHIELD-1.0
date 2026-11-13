package com.royalshield.app.auth

fun googleAuthErrorMessage(errorCode: String?, detail: String?): String = when {
    detail?.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) == true ->
        "Google sign-in is not enabled in Firebase Authentication."
    errorCode == "ERROR_NETWORK_REQUEST_FAILED" ->
        "Google sign-in could not reach Firebase. Check your connection and try again."
    errorCode == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
        "An account already exists with this email using another sign-in method."
    errorCode == "ERROR_USER_DISABLED" ->
        "This account is disabled. Contact Royal Shield support."
    else -> "Google authentication failed. Please try again."
}
