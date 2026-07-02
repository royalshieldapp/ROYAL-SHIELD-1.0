package com.royalshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.royalshield.app.ui.theme.Royal_shieldTheme
import com.royalshield.app.ui.screens.RegistrationScreen

class GoogleSignInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Royal_shieldTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    RegistrationScreen(
                        onRegistrationSuccess = { finish() },
                        onNavigateToPhoneAuth = { /* For now, do nothing or finish */ finish() }
                    )
                }
            }
        }
    }
}
