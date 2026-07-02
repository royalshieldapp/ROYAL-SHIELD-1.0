package com.royalshield.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import com.royalshield.app.managers.PreferencesManager
import com.royalshield.app.ui.components.CyberButtonRect
import com.royalshield.app.ui.theme.RoyalGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    
    var darkMode by remember { mutableStateOf(PreferencesManager.isDarkModeEnabled()) }
    var notifications by remember { mutableStateOf(PreferencesManager.areNotificationsEnabled()) }
    var sound by remember { mutableStateOf(PreferencesManager.isSoundEnabled()) }
    var location by remember { mutableStateOf(PreferencesManager.isLocationEnabled()) }
    var backgroundCamera by remember { mutableStateOf(PreferencesManager.isBackgroundCameraEnabled()) }

    val gradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.surface
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Background Image
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.bg_advanced_settings),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Dark Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Appearance Section
                AdvancedSettingsSection(title = "Appearance", icon = Icons.Default.Palette) {
                    var selectedTheme by remember { mutableStateOf(PreferencesManager.getThemeStyle()) }
                    
                    Text("Theme Style", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        val themes = listOf(
                            Triple("Neon", Color(0xFF7B42F6), Color(0xFF0D0D10)),
                            Triple("Gold", Color(0xFFFFD700), Color(0xFF2D1A00)),
                            Triple("Obsidian", Color(0xFF00B4D8), Color(0xFF0A0A0A)),
                            Triple("Cyber", Color(0xFF00FFCC), Color(0xFF050505))
                        )
                        items(themes.size) { index ->
                            val (name, accent, bg) = themes[index]
                            val isSelected = selectedTheme == name
                            
                            ElevatedCard(
                                onClick = { 
                                    selectedTheme = name
                                    PreferencesManager.setThemeStyle(name)
                                },
                                modifier = Modifier
                                    .size(width = 100.dp, height = 80.dp)
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, Color.White, RoundedCornerShape(12.dp)) else Modifier
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = bg)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(modifier = Modifier.size(24.dp).background(accent, RoundedCornerShape(4.dp)))
                                        Text(name, color = Color.White, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }

                    AdvancedSwitchItem(
                        title = "Dark Mode",
                        description = "強制 (Forced) dark theme across the app",
                        checked = darkMode,
                        onCheckedChange = {
                            darkMode = it
                            PreferencesManager.setDarkModeEnabled(it)
                            android.widget.Toast.makeText(context, if(it) "Dark Mode Activated" else "Dark Mode Deactivated", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // 2. Notifications & Sound Section
                AdvancedSettingsSection(title = "Notifications & Sound", icon = Icons.Default.Notifications) {
                    AdvancedSwitchItem(
                        title = "Push Notifications",
                        description = "Receive alerts for security events",
                        checked = notifications,
                        onCheckedChange = {
                            notifications = it
                            PreferencesManager.setNotificationsEnabled(it)
                            android.widget.Toast.makeText(context, if(it) "Notifications Enabled" else "Notifications Disabled", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                    AdvancedSwitchItem(
                        title = "Sound Alerts",
                        description = "Enable sounds for critical alerts",
                        checked = sound,
                        onCheckedChange = {
                            sound = it
                            PreferencesManager.setSoundEnabled(it)
                            android.widget.Toast.makeText(context, if(it) "Sound Alerts Enabled" else "Sound Alerts Disabled", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // 3. System Permissions Section
                AdvancedSettingsSection(title = "System Permissions", icon = Icons.Default.Gavel) {
                    AdvancedSwitchItem(
                        title = "Location Services",
                        description = "Allow app to access real-time GPS",
                        checked = location,
                        onCheckedChange = {
                            location = it
                            PreferencesManager.setLocationEnabled(it)
                            android.widget.Toast.makeText(context, if(it) "GPS Access Enabled" else "GPS Access Disabled", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                    AdvancedSwitchItem(
                        title = "Background Camera",
                        description = "Allow emergency photo capture",
                        checked = backgroundCamera,
                        onCheckedChange = {
                            backgroundCamera = it
                            PreferencesManager.setBackgroundCameraEnabled(it)
                            android.widget.Toast.makeText(context, if(it) "Background Camera Enabled" else "Background Camera Disabled", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    CyberButtonRect(
                        text = "Open System Settings",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        color = Color.White,
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // D-ID Agent Config Section
                AdvancedSettingsSection(title = "AI Avatar Agent (D-ID)", icon = Icons.Default.Face) {
                    var apiKeyInput by remember { mutableStateOf(PreferencesManager.getDidApiKey() ?: "") }
                    var agentIdInput by remember { mutableStateOf(PreferencesManager.getDidAgentId() ?: "") }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFF2A2A2A).copy(alpha = 0.95f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Configure your D-ID Agent credentials to enable the 3D talking avatar.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))

                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                label = { Text("D-ID API Key", color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = RoyalGold,
                                    unfocusedBorderColor = Color.Gray
                                )
                            )

                            OutlinedTextField(
                                value = agentIdInput,
                                onValueChange = { agentIdInput = it },
                                label = { Text("D-ID Agent ID", color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = RoyalGold,
                                    unfocusedBorderColor = Color.Gray
                                )
                            )

                            CyberButtonRect(
                                text = "Save D-ID Credentials",
                                icon = Icons.Default.Save,
                                color = RoyalGold,
                                onClick = {
                                    PreferencesManager.saveDidApiKey(apiKeyInput)
                                    PreferencesManager.saveDidAgentId(agentIdInput)
                                    android.widget.Toast.makeText(context, "D-ID Config Saved", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 4. Support & Feedback Section
                AdvancedSettingsSection(title = "Support & Feedback", icon = Icons.Default.Favorite) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFF2A2A2A).copy(alpha = 0.95f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Enjoying Royal Shield?", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Your feedback helps us improve the security of thousands of users.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                            
                            CyberButtonRect(
                                text = "Rate Us on Play Store",
                                icon = Icons.Default.Star,
                                color = RoyalGold,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun AdvancedSettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
fun AdvancedSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF2A2A2A).copy(alpha = 0.95f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                Text(description, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
