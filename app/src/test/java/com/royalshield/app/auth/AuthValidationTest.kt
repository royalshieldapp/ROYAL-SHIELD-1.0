package com.royalshield.app.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {
    @Test
    fun validatesAndNormalizesEmail() {
        assertTrue(isValidEmail(" User.Name+tag@example.com "))
        assertEquals("user.name+tag@example.com", normalizeEmail(" User.Name+tag@example.com "))
        assertFalse(isValidEmail("user@example"))
    }

    @Test
    fun requiresStrongPassword() {
        assertNull(passwordValidationMessage("Royal123"))
        assertEquals(
            "Password must contain an uppercase letter",
            passwordValidationMessage("royal123")
        )
        assertEquals(
            "Password must contain a number",
            passwordValidationMessage("RoyalShield")
        )
    }

    @Test
    fun validatesE164PhoneAfterRemovingFormatting() {
        assertEquals("+13055551234", normalizeE164Phone("+1 (305) 555-1234"))
        assertTrue(isValidE164Phone("+1 (305) 555-1234"))
        assertFalse(isValidE164Phone("3055551234"))
    }

    @Test
    fun acceptsOnlySixDigitOtp() {
        assertTrue(isValidOtp("123456"))
        assertFalse(isValidOtp("12345"))
        assertFalse(isValidOtp("12345A"))
    }
}
