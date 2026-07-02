package com.royalshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.royalshield.app.ui.screens.CourseScreen
import com.royalshield.app.ui.theme.Royal_shieldTheme

class CourseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Royal_shieldTheme {
                CourseScreen(onBackPressed = { finish() })
            }
        }
    }
}
