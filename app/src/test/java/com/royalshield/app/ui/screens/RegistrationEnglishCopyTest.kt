package com.royalshield.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class RegistrationEnglishCopyTest {

    @Test
    fun registrationVisibleCopyIsEnglishOnly() {
        val screenPath = sequenceOf(
            Path.of("src", "main", "java", "com", "royalshield", "app", "ui", "screens", "RegistrationScreen.kt"),
            Path.of("app", "src", "main", "java", "com", "royalshield", "app", "ui", "screens", "RegistrationScreen.kt"),
        ).first { Files.isRegularFile(it) }
        val heroPath = screenPath.resolveSibling("RegistrationOrbHero.kt")
        val source = listOf(screenPath, heroPath)
            .filter(Files::isRegularFile)
            .joinToString("\n") { path ->
                String(Files.readAllBytes(path), StandardCharsets.UTF_8)
            }

        listOf(
            "Your security agent is waiting.",
            "Sign up to configure available security, privacy, and family protection features.",
            "SECURITY AGENT • LOCKED",
            "Choose how to continue",
            "Apple — COMING SOON",
            "GitHub — COMING SOON",
            "Facebook — COMING SOON",
            "Email — COMING SOON",
            "Security Agent Locked",
            "Continue with Phone or Google to begin setup. Feature availability depends on your plan, device permissions, and configured services.",
            "GOT IT",
        ).forEach { approvedCopy ->
            assertTrue("Missing approved English copy: $approvedCopy", source.contains(approvedCopy))
        }

        listOf(
            "próximamente",
            "Regístrate",
            "Teléfono",
            "Agente bloqueado",
            "ENTENDIDO",
            "Tu agente",
        ).forEach { prohibitedCopy ->
            assertFalse("Registration still contains Spanish copy: $prohibitedCopy", source.contains(prohibitedCopy))
        }
    }
}
