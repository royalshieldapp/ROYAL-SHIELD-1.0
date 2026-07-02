package com.royalshield.app.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build

/**
 * Simple representation of a bottom navigation item.
 * Only the fields required by the current codebase are included.
 */
data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val title: String
) {
    companion object {
        val Home = BottomNavItem("home", Icons.Default.Home, "Home")
        val GlobalMap = BottomNavItem("global_map", Icons.Default.Map, "Map")
        val CyberMap = BottomNavItem("cyber_map", Icons.Default.Public, "Cyber")
        val Automation = BottomNavItem("automation", Icons.Default.Build, "Automation")
        val Settings = BottomNavItem("settings", Icons.Default.Settings, "Settings")
        // Add other items as needed
    }
}
