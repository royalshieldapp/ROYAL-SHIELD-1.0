package com.royalshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.royalshield.app.ui.screens.AdvancedSettingsScreen
import com.royalshield.app.ui.theme.Royal_shieldTheme

class AdvancedSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Royal_shieldTheme {
                AdvancedSettingsScreen(onBackPressed = { finish() })
            }
        }
    }
}
