package com.royalshield.app.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.royalshield.app.ui.components.CyberButtonRect
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.CyberCyan
import com.royalshield.app.managers.PreferencesManager
import android.widget.Toast
import com.royalshield.app.SosManager

private enum class SettingsContentWindow(val title: String) {
    ContactSupport("Contact Support"),
    PrivacyPolicy("Privacy Policy"),
    TermsAndConditions("Terms & Conditions")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToLoyalty: () -> Unit = {},
    onNavigateToAdvancedSettings: () -> Unit = {},
    onNavigateToCourses: () -> Unit = {},
    onNavigateToFundamentals: () -> Unit = {},
    onNavigateToReferral: () -> Unit = {}
) {
    val context = LocalContext.current
    // Ensure PreferencesManager is initialized (safe to call multiple times if handled safely,
    // but better if global. Assuming it is safe or App-level init)
    // Here accessing properties directly might crash if not init.
    // PreferencesManager.init(context) was called in MainActivity.

    var vibrationEnabled by remember { mutableStateOf(PreferencesManager.isVibrationEnabled()) }
    var historyEnabled by remember { mutableStateOf(PreferencesManager.isHistoryEnabled()) }
    var activeContentWindow by remember { mutableStateOf<SettingsContentWindow?>(null) }



    // Background Image
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.bg_settings),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark Overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Settings",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.btn_back_gold),
                                contentDescription = "Back",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp) // Professional section spacing
            ) {
                // 1. Appearance Section
                var darkModeEnabled by remember { mutableStateOf(PreferencesManager.isDarkModeEnabled()) }
                SettingsSection(
                    title = "🎨 Appearance",
                    description = "Customize the look and feel"
                ) {
                    SettingsSwitchItem(
                        title = "Dark Mode",
                        description = "Enable dark theme for low light environments",
                        checked = darkModeEnabled,
                        onCheckedChange = { enabled ->
                            darkModeEnabled = enabled
                            PreferencesManager.setDarkModeEnabled(enabled)
                        }
                    )
                }

                // 2. Subscription Section
                SettingsSection(
                    title = "💎 Subscription",
                    description = "Manage your Elite plan"
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToSubscription),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFF1A1A1A).copy(alpha = 0.85f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Diamond, "Elite", tint = Color(0xFFFFD700))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Royal Shield Elite", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                                    Text("Manage subscription", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.btn_forward_gold),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 3. Loyalty Program Section
                SettingsSection(
                    title = "✨ Loyalty",
                    description = "Rewards program"
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToLoyalty),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFF1E3A8A).copy(alpha = 0.85f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, "Loyalty", tint = Color.White)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Royal Shield Loyalty", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("View benefits and level", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                }
                            }
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.btn_forward_gold),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Referral Program
                    Spacer(modifier = Modifier.height(8.dp))
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToReferral),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFF1A2A1A).copy(alpha = 0.85f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, "Referral", tint = RoyalGold)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Refer & Earn", color = RoyalGold, fontWeight = FontWeight.Bold)
                                    Text("Invite friends, get rewards", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.btn_forward_gold),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }


                SettingsSection(
                    title = "🛠️ Advanced",
                    description = "System configurations & expert options"
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFF2A2A2A).copy(alpha = 0.95f)
                        )
                    ) {
                        SettingsNavigationItem(
                            title = "Advanced System Settings",
                            icon = Icons.Default.Settings,
                            onClick = onNavigateToAdvancedSettings
                        )
                    }

                    // From SOS Button Behavior section (preserving zero deletion)
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsSwitchItem(
                        title = "Vibration on SOS",
                        description = "The device will vibrate briefly when the panic button is pressed",
                        checked = vibrationEnabled,
                        onCheckedChange = { enabled ->
                            vibrationEnabled = enabled
                            PreferencesManager.setVibrationEnabled(enabled)
                            android.widget.Toast.makeText(context, if(enabled) "Vibration Activated" else "Vibration Deactivated", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    CyberButtonRect(
                        text = "Test SOS Configuration",
                        icon = Icons.Default.BugReport,
                        color = RoyalGold,
                        onClick = {
                            SosManager.triggerSilentSos(context)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 5. Resources & Help Section
                SettingsSection(
                    title = "📚 Resources & Help",
                    description = "Education, Training & Knowledge Hub"
                ) {
                    // Card 1: Courses
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onNavigateToCourses),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFF2A2A2A).copy(alpha = 0.95f)
                        )
                    ) {
                        SettingsNavigationItem(
                            title = "Courses",
                            icon = Icons.Default.School,
                            onClick = onNavigateToCourses
                        )
                    }

                    // Card 2: Cybersecurity Fundamentals
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onNavigateToFundamentals),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFF2A2A2A).copy(alpha = 0.95f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.img_icon_shield_gold),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Cybersecurity Fundamentals",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.btn_forward_gold),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Card 3: Help Center (Moved from Resource Section)
                    CyberButtonRect(
                        text = "Help Center",
                        icon = Icons.Default.Help,
                        color = Color(0xFF00E676),
                        onClick = {
                            Toast.makeText(context, "Redirecting to Support Center...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                }

                // 6. Privacy & History Section
                SettingsSection(
                    title = "🔒 Privacy & History",
                    description = "Manage your event logs and data storage"
                ) {
                    SettingsSwitchItem(
                        title = "Save event history",
                        description = "Records date, time, and location of each SOS event for later consultation",
                        checked = historyEnabled,
                        onCheckedChange = { enabled ->
                            historyEnabled = enabled
                            PreferencesManager.setHistoryEnabled(enabled)
                            android.widget.Toast.makeText(context, if(enabled) "History Enabled" else "History Disabled", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // 7. Legal Section
                SettingsSection(
                    title = "⚖️ Legal",
                    description = "Policies, terms, and conditions"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CyberButtonRect(
                            text = "Privacy Policy",
                            icon = Icons.Default.Security,
                            color = CyberCyan,
                            onClick = {
                                activeContentWindow = SettingsContentWindow.PrivacyPolicy
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        CyberButtonRect(
                            text = "Terms & Conditions",
                            icon = Icons.Default.Description,
                            color = RoyalGold,
                            onClick = {
                                activeContentWindow = SettingsContentWindow.TermsAndConditions
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 8. Support & Contact Section
                SettingsSection(
                    title = "📧 Support & Contact",
                    description = "Get in touch with Royal Shield"
                ) {
                    CyberButtonRect(
                        text = "Contact Support",
                        icon = Icons.Default.Email,
                        color = CyberCyan,
                        onClick = {
                            activeContentWindow = SettingsContentWindow.ContactSupport
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 9. Social Section (Placeholder)
                SettingsSection(
                    title = "🌐 Social",
                    description = "Follow our neural updates"
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SocialActionButton(
                            text = "Instagram",
                            iconRes = com.royalshield.app.R.drawable.ic_social_instagram,
                            color = Color(0xFFE1306C),
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://instagram.com/royalshieldapp"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        SocialActionButton(
                            text = "Facebook",
                            iconRes = com.royalshield.app.R.drawable.ic_social_facebook,
                            color = Color(0xFF1877F2),
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.facebook.com/profile.php?id=61573434707140"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        SocialActionButton(
                            text = "YouTube",
                            iconRes = com.royalshield.app.R.drawable.ic_social_youtube,
                            color = Color(0xFFFF0000),
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://youtube.com/@royalshield"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 10. Information Section
                SettingsSection(
                    title = "ℹ️ Information",
                    description = "System & Developer details"
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFF2A2A2A).copy(alpha = 0.95f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfoRow(label = "Version", value = "2.0")
                            InfoRow(label = "Developer", value = "Royal Shield Team")
                        }
                    }
                }

                // 11. Security Note
                SecurityNote()

                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        activeContentWindow?.let { window ->
            EmptySettingsContentDialog(
                title = window.title,
                onDismiss = { activeContentWindow = null }
            )
        }
    }
}
}

@Composable
private fun EmptySettingsContentDialog(
    title: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = RoyalGold)
            }
        },
        title = {
            Text(
                text = title,
                color = RoyalGold,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(0xFF101014), RoundedCornerShape(12.dp))
            )
        },
        containerColor = Color(0xFF151515),
        titleContentColor = RoyalGold,
        textContentColor = Color.White
    )
}

@Composable
private fun SocialActionButton(
    text: String,
    iconRes: Int,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .height(56.dp)
            .border(BorderStroke(1.dp, color.copy(alpha = 0.65f)), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF151515).copy(alpha = 0.95f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = iconRes),
                contentDescription = text,
                modifier = Modifier.size(30.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
            Text(
                text = text.uppercase(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
    }
}


@Composable
fun SettingsSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        content()
    }
}

@Composable
fun SettingsSwitchItem(
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
fun SecurityNote() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.titleLarge
            )
            Column {
                Text(
                    text = "Security Note",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5252)
                )
                Text(
                    text = "The SOS button opens your messaging app. Make sure you have an emergency contact configured before using it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun SettingsNavigationItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.btn_forward_gold),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}



