package com.royalshield.app.security

import org.junit.Assert.assertEquals
import org.junit.Test

class AppCheckConfigurationTest {

    @Test
    fun adbDebugBuildUsesRegisteredDebugProvider() {
        assertEquals(AppCheckProviderMode.DEBUG, appCheckProviderMode(isDebugBuild = true))
    }

    @Test
    fun productionBuildUsesPlayIntegrity() {
        assertEquals(
            AppCheckProviderMode.PLAY_INTEGRITY,
            appCheckProviderMode(isDebugBuild = false),
        )
    }
}
