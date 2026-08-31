package com.royalshield.app.ui.screens

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupFlowContractTest {

    @Test
    fun everyLaunchShowsRegistrationAfterSplashWithoutRemovingOnboardingScreens() {
        val source = File("src/main/java/com/royalshield/app/MainActivity.kt").readText()

        assertTrue(source.contains("AppState.SPLASH -> SplashScreen {\n                currentState = AppState.REGISTRATION"))
        assertTrue(source.contains("onRegistrationSuccess = { currentState = AppState.PREMIUM_PROMO }"))
        assertTrue(source.contains("AppState.SECURITY_SETUP"))
        assertTrue(source.contains("onComplete = { currentState = AppState.MAIN }"))
    }
}
