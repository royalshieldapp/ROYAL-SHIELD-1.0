package com.royalshield.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class RegistrationAccessPolicyTest {

    @Test
    fun onlyConfiguredProvidersCanStartAuthentication() {
        assertTrue(RegistrationProvider.PHONE.isAvailable)
        assertTrue(RegistrationProvider.GOOGLE.isAvailable)
        assertFalse(RegistrationProvider.APPLE.isAvailable)
        assertFalse(RegistrationProvider.GITHUB.isAvailable)
        assertFalse(RegistrationProvider.FACEBOOK.isAvailable)
        assertFalse(RegistrationProvider.EMAIL.isAvailable)
    }

    @Test
    fun lockedOrbAlwaysRequiresARealRegistration() {
        assertFalse(canEnterProtectedApp(hasAuthenticatedUser = false))
        assertTrue(canEnterProtectedApp(hasAuthenticatedUser = true))
    }

    @Test
    fun registrationScreenContainsNoFakeLoginTransition() {
        val source = registrationSource("RegistrationScreen.kt")

        assertFalse(source.contains("Switching to Login"))
    }

    @Test
    fun lockedHeroCanOnlyRequestLockedInformation() {
        val source = registrationSource("RegistrationOrbHero.kt")

        assertTrue(source.contains("onLockedInfo: () -> Unit"))
        assertTrue(source.contains("onClick = onLockedInfo"))
        assertFalse(source.contains("onRegistrationSuccess"))
        assertFalse(source.contains("onNavigateToPhoneAuth"))
        assertFalse(source.contains("FirebaseAuth"))
        assertFalse(source.contains("GoogleSignIn"))
    }

    @Test
    fun registrationPresentsOnlyPhoneAndGoogleAsAvailableActions() {
        val source = registrationSource("RegistrationScreen.kt")

        listOf("PHONE", "GOOGLE").forEach { provider ->
            assertTrue(
                "$provider must use the configured provider availability",
                source.contains("enabled = RegistrationProvider.$provider.isAvailable"),
            )
        }
        listOf("APPLE", "GITHUB", "FACEBOOK", "EMAIL").forEach { provider ->
            assertTrue(
                "$provider must remain tied to its disabled provider policy",
                source.contains("enabled = RegistrationProvider.$provider.isAvailable"),
            )
        }
        assertTrue(source.windowed("COMING SOON".length).count { it == "COMING SOON" } >= 4)
    }

    private fun registrationSource(fileName: String): String {
        val source = sequenceOf(
            Path.of("src", "main", "java", "com", "royalshield", "app", "ui", "screens", fileName),
            Path.of("app", "src", "main", "java", "com", "royalshield", "app", "ui", "screens", fileName),
        ).first { Files.isRegularFile(it) }
        return String(Files.readAllBytes(source), StandardCharsets.UTF_8)
    }
}
