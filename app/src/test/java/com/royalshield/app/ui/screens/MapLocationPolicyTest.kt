package com.royalshield.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class MapLocationPolicyTest {
    @Test
    fun `location state explains why the real device point is absent`() {
        assertEquals(
            MapLocationStatus.LOCATION_OFF,
            resolveMapLocationStatus(locationEnabled = false, hasPermission = true, hasFix = false),
        )
        assertEquals(
            MapLocationStatus.PERMISSION_REQUIRED,
            resolveMapLocationStatus(locationEnabled = true, hasPermission = false, hasFix = false),
        )
        assertEquals(
            MapLocationStatus.WAITING_FOR_FIX,
            resolveMapLocationStatus(locationEnabled = true, hasPermission = true, hasFix = false),
        )
        assertEquals(
            MapLocationStatus.AVAILABLE,
            resolveMapLocationStatus(locationEnabled = true, hasPermission = true, hasFix = true),
        )
    }
}
