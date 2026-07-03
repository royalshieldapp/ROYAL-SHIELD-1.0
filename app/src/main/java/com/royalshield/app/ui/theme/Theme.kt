package com.royalshield.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 1. NEON (Default - Cyber/Purple)
private val NeonTheme = darkColorScheme(
    primary = Color(0xFF7B42F6), // Purple
    secondary = NeonBlue,
    tertiary = RoyalGold,
    background = Color(0xFF0D0D10),
    surface = Color(0xFF16161C),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    error = NeonRed
)

// 2. GOLD (Luxury)
private val GoldTheme = darkColorScheme(
    primary = RoyalGold,
    secondary = Color(0xFFB8860B),
    tertiary = Color.White,
    background = Color(0xFF050505), // Deep Black
    surface = Color(0xFF121212),
    onPrimary = Color.Black,
    onBackground = Color(0xFFF0EAD6), // Eggshell
    onSurface = RoyalGold,
    error = Color(0xFF800000)
)

// 3. OBSIDIAN (Clean Blue/Black)
private val ObsidianTheme = darkColorScheme(
    primary = Color(0xFF00B4D8), // Light Blue
    secondary = Color(0xFF0077B6),
    tertiary = Color.White,
    background = Color.Black,
    surface = Color(0xFF111111),
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    error = NeonRed
)

// 4. CYBER (Matrix/Hacker Green)
private val CyberTheme = darkColorScheme(
    primary = Color(0xFF00FF94), // Neon Green
    secondary = Color(0xFF00CC7A),
    tertiary = Color(0xFF003300),
    background = Color(0xFF050505),
    surface = Color(0xFF0A1A0A),
    onPrimary = Color.Black,
    onBackground = Color(0xFFCCFFCC),
    onSurface = Color(0xFF00FF94),
    error = NeonRed
)

// 5. GOLD LIGHT (White/Gold Luxury)
private val GoldLightTheme = lightColorScheme(
    primary = RoyalGold,
    secondary = Color(0xFFDAA520),
    tertiary = Color(0xFF1E1E1E), // Dark accents
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF1C1C1E), // Soft Black
    onSurface = Color(0xFF1C1C1E),
    error = Color(0xFFB00020)
)

@Composable
fun Royal_shieldTheme(
    style: String = "Neon", // Added style parameter
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        style == "Gold" && !darkTheme -> GoldLightTheme
        style == "Gold" -> GoldTheme
        style == "Obsidian" -> ObsidianTheme
        style == "Cyber" -> CyberTheme
        else -> NeonTheme // Default
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}