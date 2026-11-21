package com.royalshield.app.ui.screens

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnTruthfulnessTest {
    @Test
    fun `server selection uses backend results only`() {
        val returned = listOf(VpnServer("US East", "US", "US", "us-east"))

        assertNull(selectVerifiedVpnServer(null, emptyList()))
        assertEquals(returned.first(), selectVerifiedVpnServer(null, returned))
        assertEquals(returned.first(), selectVerifiedVpnServer(returned.first(), returned))
    }

    @Test
    fun `vpn screen contains no static servers or invented latency`() {
        val source = File("src/main/java/com/royalshield/app/ui/screens/VpnScreen.kt").readText()
        val navigation = File("src/main/java/com/royalshield/app/ui/navigation/Navigation.kt").readText()
        val vpnRoute = navigation.substringAfter("composable(\"vpn\")").substringBefore("composable(\"tracking_shield\")")

        assertFalse(source.contains("staticServers"))
        assertFalse(source.contains("28 ms"))
        assertFalse(source.contains("28ms"))
        assertTrue(source.contains("mutableStateOf<VpnServer?>(null)"))
        assertFalse(vpnRoute.contains("FeatureAvailabilityGate"))
        assertTrue(vpnRoute.contains("VpnScreen("))
    }
}
