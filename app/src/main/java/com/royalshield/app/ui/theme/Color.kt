package com.royalshield.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Gold Pro Luxury Tokens (Signature Brand Specification)
val GoldProBackground = Color(0xFF050505) // Deep Titanium Obsidian
val GoldProSurface = Color(0xFF121418) // Smoked Obsidian Base
val GoldProSurfaceGlass = Color(0xE6121418) // 90% opacity smoked acrylic glass
val GoldProSurfaceElevated = Color(0xFF1A1D24)
val GoldProBorder = Color(0x59FFD700) // 35% alpha gold hairline
val GoldProBorderSubtle = Color(0x26FFD700) // 15% alpha
val GoldProPrimary = Color(0xFFFFD700) // 24K Royal Gold
val GoldProSecondary = Color(0xFFE5C158) // Aureum Gold
val GoldProDark = Color(0xFFB8860B) // Antique Dark Gold
val GoldProTextPrimary = Color(0xFFF5F5F0) // High-contrast Eggshell White
val GoldProTextSecondary = Color(0xFFD4AF37) // Metallic Gold Label
val GoldProTextMuted = Color(0xFF9E9E9E)

val GoldProMetallicBrush = Brush.linearGradient(
    listOf(Color(0xFFFFDF73), Color(0xFFFFD700), Color(0xFFB8860B))
)
val GoldProGlassBrush = Brush.verticalGradient(
    listOf(Color(0xFF1A1D24).copy(alpha = 0.95f), Color(0xFF0D0F13).copy(alpha = 0.90f))
)
val GoldProBorderBrush = Brush.linearGradient(
    listOf(Color(0xFFFFD700).copy(alpha = 0.55f), Color(0xFFB8860B).copy(alpha = 0.18f))
)

// Tesla-inspired Minimalist Palette
val TeslaBlack = Color(0xFF171A20) // Main Background
val TeslaDarkGrey = Color(0xFF222222) // Human Interface Grey
val TeslaLightGrey = Color(0xFF393C41) // Secondary Elements
val TeslaWhite = Color(0xFFF4F4F4) // Text & Icons
val TeslaRed = Color(0xFFE82127) // Primary Actions / Alerts
val TeslaBlue = Color(0xFF3E6AE1) // Secondary Actions (like Ocean Blue paint)
val TeslaGreen = Color(0xFF3DAF5D) // Success states

// Mapping to Theme Colors for compatibility
val RoyalGold = Color(0xFFFFD700) // Real Gold for Elite
val NeonGold = Color(0xFFFFD700)
val NeonPrimary = TeslaWhite
val NeonSecondary = TeslaLightGrey
val RoyalBg = TeslaBlack
val RoyalBgAlt = TeslaDarkGrey
val GlassNormal = TeslaDarkGrey.copy(alpha = 0.9f)
val BorderColor = Color.White.copy(alpha = 0.1f)
val NeonRed = TeslaRed
val SafeGreen = TeslaGreen

val Obsidian = TeslaBlack
val SpaceDeep = TeslaBlack
val CyberCyan = TeslaBlue
val RoyalBlue = TeslaBlue // Alias for consistency
val NeonBlue = Color(0xFF00E5FF) // Cyber Blue
// Neon Palette for Dashboard Cards
val NeonPink = Color(0xFFFF1493)
val NeonOrange = Color(0xFFFF5722)
val NeonWhite = Color.White
val NeonLemon = Color(0xFF76FF03)
val NeonAqua = Color(0xFF00E5FF)
val NeonGreen = Color(0xFF00E676)
val NeonRedHighlight = Color(0xFFFF1744)

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
