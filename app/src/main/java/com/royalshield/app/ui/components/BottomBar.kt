package com.royalshield.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.royalshield.app.ui.theme.*

import androidx.compose.ui.res.painterResource
import com.royalshield.app.R

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector? = null,
    val iconResId: Int? = null,
    val color: Color
) {
    object Home : BottomNavItem("home", "Home", null, com.royalshield.app.R.drawable.nav_home, RoyalGold)
    object GlobalMap : BottomNavItem("global_map", "Map", null, com.royalshield.app.R.drawable.nav_location, NeonRed)
    object CyberMap : BottomNavItem("cyber_map", "Cyber", null, com.royalshield.app.R.drawable.nav_shield, CyberCyan)
    object Automation : BottomNavItem("automation", "Auto", null, com.royalshield.app.R.drawable.nav_automation, SafeGreen)
    object Settings : BottomNavItem("settings", "Settings", null, com.royalshield.app.R.drawable.nav_settings, Color.White)
}

@Composable
fun RoyalShieldBottomBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.GlobalMap,
        BottomNavItem.CyberMap,
        BottomNavItem.Automation,
        BottomNavItem.Settings
    )

    NavigationBar(
        containerColor = SpaceDeep.copy(alpha = 0.95f),
        contentColor = Color.White
    ) {
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry.value?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    if (item.iconResId != null) {
                        Icon(
                            painter = painterResource(id = item.iconResId),
                            contentDescription = item.title,
                            modifier = androidx.compose.ui.Modifier.size(48.dp), // Increased size
                            tint = Color.Unspecified // Keep original colors for the PNG
                        )
                    } else if (item.icon != null) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = androidx.compose.ui.Modifier.size(40.dp) // Increased size
                        )
                    }
                },
                label = { Text(text = item.title) },
                selected = currentRoute == item.route,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = item.color,
                    selectedTextColor = item.color,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = if (item == BottomNavItem.Automation) Color.Transparent else item.color.copy(alpha = 0.1f)
                ),
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
