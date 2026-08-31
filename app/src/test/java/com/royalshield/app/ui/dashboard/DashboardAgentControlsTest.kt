package com.royalshield.app.ui.dashboard

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardAgentControlsTest {

    @Test
    fun armedControlRequiresConfirmationBeforeDisarming() {
        val presentation = dashboardAgentControlPresentation(armed = true)

        assertEquals("SYSTEM ARMED", presentation.label)
        assertTrue(presentation.requiresConfirmation)
    }

    @Test
    fun disarmedControlCanArmDirectly() {
        val presentation = dashboardAgentControlPresentation(armed = false)

        assertEquals("SYSTEM DISARMED", presentation.label)
        assertFalse(presentation.requiresConfirmation)
    }

    @Test
    fun dashboardUsesAgentCenterWithoutLegacyFloatingControls() {
        val source = File("src/main/java/com/royalshield/app/ui/screens/DashboardScreen.kt").readText()

        assertTrue(source.contains("DashboardAgentCenter("))
        assertTrue(source.contains("onVoiceClick = { showVoiceAssistant = true }"))
        assertTrue(source.contains("onChatClick = { showTextAssistant = true }"))
        assertFalse(source.contains("Floating Chat Button"))
        assertFalse(source.contains("Floating Mic Button"))
    }
}
