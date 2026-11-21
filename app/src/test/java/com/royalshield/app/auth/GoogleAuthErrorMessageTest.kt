package com.royalshield.app.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleAuthErrorMessageTest {

    @Test
    fun configurationFailureExplainsFirebaseProviderRequirement() {
        assertEquals(
            "Google sign-in is not enabled in Firebase Authentication.",
            googleAuthErrorMessage(
                errorCode = "ERROR_INTERNAL_ERROR",
                detail = "An internal error has occurred. [ CONFIGURATION_NOT_FOUND ]",
            ),
        )
    }

    @Test
    fun networkFailureRemainsActionableWithoutLeakingProviderDetails() {
        assertEquals(
            "Google sign-in could not reach Firebase. Check your connection and try again.",
            googleAuthErrorMessage(errorCode = "ERROR_NETWORK_REQUEST_FAILED", detail = null),
        )
    }
}
