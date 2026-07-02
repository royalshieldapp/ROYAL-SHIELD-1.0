package com.royalshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.royalshield.app.ui.screens.LocationTimelineScreen
import com.royalshield.app.ui.theme.Royal_shieldTheme

class LocationTimelineActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Royal_shieldTheme {
                LocationTimelineScreen(
                    onBackPressed = { finish() }
                )
            }
        }
    }
}
