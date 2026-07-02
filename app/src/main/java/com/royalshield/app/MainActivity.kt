package com.royalshield.app

import java.util.concurrent.atomic.AtomicInteger
import java.util.Date

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.luminance
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.royalshield.app.ui.navigation.RoyalShieldNavGraph
import com.royalshield.app.ui.components.RoyalShieldBottomBar
import com.royalshield.app.ui.theme.*
import com.royalshield.app.managers.TwilioManager
import com.royalshield.app.managers.BillingManager
import com.royalshield.app.managers.PreferencesManager
import com.royalshield.app.ui.screens.PremiumEliteScreen
import com.royalshield.app.ui.screens.RegistrationScreen
import com.royalshield.app.ui.screens.PhoneAuthScreen
import com.royalshield.app.services.RoyalShieldService
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    
    // State to hold the current theme style AND dark mode
    private val _themeStyle = mutableStateOf("Neon")
    private val _isDarkMode = mutableStateOf(true)

    // Listener for SharedPreferences changes
    private val preferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PreferencesManager.KEY_THEME_STYLE) {
            _themeStyle.value = PreferencesManager.getThemeStyle()
        }
        if (key == "key_dark_mode_enabled") { // Hardcoded key check matching PreferencesManager
            _isDarkMode.value = PreferencesManager.isDarkModeEnabled()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferencesManager.init(applicationContext)
        
        // Initialize state with current preference
        _themeStyle.value = PreferencesManager.getThemeStyle()
        _isDarkMode.value = PreferencesManager.isDarkModeEnabled()
        
        // Register listener
        PreferencesManager.registerListener(preferenceListener)
        
        checkNotificationPermission()
        
        enableEdgeToEdge()
        setContent {
            // Pass the state value to the theme
            Royal_shieldTheme(
                style = _themeStyle.value, 
                darkTheme = _isDarkMode.value
            ) {
                AppMainFlow()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        PreferencesManager.unregisterListener(preferenceListener)
    }
}

enum class AppState {
    SPLASH, PREMIUM_PROMO, REGISTRATION, PHONE_AUTH, MAIN
}

@Composable
fun AppMainFlow() {
    var currentState by remember { mutableStateOf(AppState.SPLASH) }
    val context = LocalContext.current
    
    // Initialize BillingManager
    val billingManager = remember { BillingManager(context) }
    
    // Initialize New Subscription System
    LaunchedEffect(Unit) {
        billingManager.initialize()
    }
    
    // Simplified transition to avoid emulator rendering issues with expensive animations
    Crossfade(targetState = currentState, label = "AppFlow") { state ->
        when (state) {
            AppState.SPLASH -> SplashScreen { 
                // Check if user is already signed in
                val currentUser = try {
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                } catch (e: Exception) { null }
                
                currentState = if (currentUser != null) AppState.MAIN else AppState.REGISTRATION
            }
            AppState.PREMIUM_PROMO -> PremiumEliteScreen(
                billingManager = billingManager,
                onContinue = { currentState = AppState.REGISTRATION },
                onPurchaseSuccess = { 
                    currentState = AppState.REGISTRATION 
                }
            )
            AppState.REGISTRATION -> RegistrationScreen(
                onRegistrationSuccess = { currentState = AppState.MAIN },
                onNavigateToPhoneAuth = { currentState = AppState.PHONE_AUTH }
            )
            AppState.PHONE_AUTH -> PhoneAuthScreen(
                onBack = { currentState = AppState.REGISTRATION },
                onAuthSuccess = { currentState = AppState.MAIN }
            )
            AppState.MAIN -> RoyalShieldApp(billingManager)
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // Animation states
    var showFirstMessage by remember { mutableStateOf(false) }
    var showSecondMessage by remember { mutableStateOf(false) }
    
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryTextColor = if (isLight) Color.Black else Color.White
    val secondaryTextColor = if (isLight) Color.DarkGray else Color.White.copy(alpha = 0.9f)
    
    LaunchedEffect(Unit) {
        delay(800) // Wait before showing first message
        showFirstMessage = true
        delay(600) // Wait before showing second message
        showSecondMessage = true
        delay(1600) // Wait before transitioning
        onTimeout()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // Main Logo Content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            
            // Adjust logotint if needed or assume logo works on both
            // If logo is white-only, valid tinting might be needed for light mode
            // For now, assuming the drawable resource is compatible or needs tint
            val logoTint = if (isLight) RoyalGold else Color.Unspecified 

            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.royal_shield_logo),
                contentDescription = "Royal Shield Logo",
                modifier = Modifier.size(180.dp),
                colorFilter = if (isLight) androidx.compose.ui.graphics.ColorFilter.tint(RoyalGold) else null
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "ROYAL SHIELD",
                color = primaryTextColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            // Animated Tagline Messages
            AnimatedVisibility(
                visible = showFirstMessage,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = tween(600)
                )
            ) {
                Text(
                    "⚡ Next-Level Cybersecurity at Your Fingertips",
                    color = RoyalGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AnimatedVisibility(
                visible = showSecondMessage,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = tween(600)
                )
            ) {
                Text(
                    "🔒 Secure Your Digital Life",
                    color = secondaryTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Bottom Elements
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 24/7 Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = RoyalGold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "24/7",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Version
            Text(
                "Version 1.0",
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun RoyalShieldApp(billingManager: com.royalshield.app.managers.BillingManager) {
    val context = LocalContext.current
    val navController = rememberNavController()

    // Service activation (Safe Mode) - Enabled
    // Service activation (Safe Mode) - Enabled
    /*
    LaunchedEffect(Unit) {
        try {
            val serviceIntent = Intent(context, RoyalShieldService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("RoyalShield", "Could not start service: ${e.message}")
        }
    }
    */

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { RoyalShieldBottomBar(navController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            RoyalShieldNavGraph(
                navController = navController,
                billingManager = billingManager,
                onTriggerSOS = {
                    com.royalshield.app.SosManager.triggerSos(context)
                }
            )
        }
    }
}

