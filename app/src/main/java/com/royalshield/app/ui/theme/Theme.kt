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

// 2. GOLD PRO (Signature Luxury Black & Gold)
private val GoldTheme = darkColorScheme(
    primary = RoyalGold,
    onPrimary = Color(0xFF050505),
    primaryContainer = Color(0xFF2A2000),
    onPrimaryContainer = Color(0xFFFFDF73),
    secondary = Color(0xFFE5C158),
    onSecondary = Color(0xFF1E1600),
    secondaryContainer = Color(0xFF3B2E05),
    onSecondaryContainer = Color(0xFFFFE088),
    tertiary = Color(0xFFB8860B),
    onTertiary = Color.White,
    background = GoldProBackground, // Deep Titanium Obsidian (#050505)
    onBackground = GoldProTextPrimary, // High-contrast Eggshell (#F5F5F0)
    surface = GoldProSurface, // Smoked Glass Base (#121418)
    onSurface = GoldProTextPrimary,
    surfaceVariant = GoldProSurfaceElevated,
    onSurfaceVariant = GoldProTextSecondary,
    outline = GoldProBorder,
    outlineVariant = GoldProBorderSubtle,
    error = Color(0xFFFF453A),
    onError = Color.White
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
    style: String = "Gold", // Gold Pro is the signature default theme
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        (style == "Gold" || style == "GoldPro" || style == "Gold Pro") && !darkTheme -> GoldLightTheme
        (style == "Gold" || style == "GoldPro" || style == "Gold Pro") -> GoldTheme
        style == "Obsidian" -> ObsidianTheme
        style == "Cyber" -> CyberTheme
        style == "Neon" -> NeonTheme
        else -> GoldTheme // Default to Gold Pro
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