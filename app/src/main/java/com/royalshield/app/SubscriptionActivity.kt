package com.royalshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.royalshield.app.ui.screens.SubscriptionScreen
import com.royalshield.app.ui.theme.Royal_shieldTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import com.royalshield.app.managers.PreferencesManager
import com.royalshield.app.managers.BillingManager

class SubscriptionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize crucial managers
        PreferencesManager.init(applicationContext)
        val billingManager = BillingManager(this)
        billingManager.initialize()
        
        setContent {
            Royal_shieldTheme {
                val isPremium by billingManager.hasPremiumAccess.collectAsState(initial = false)
                SubscriptionScreen(
                    isPremium = isPremium,
                    billingManager = billingManager
                )
            }
        }
    }
}
