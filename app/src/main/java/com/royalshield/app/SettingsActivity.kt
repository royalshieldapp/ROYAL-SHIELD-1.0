package com.royalshield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.SettingsSuggest

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.royalshield.app.ui.theme.Royal_shieldTheme
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.managers.PreferencesManager

/**
 * Activity to configure application preferences
 * Allows activating/deactivating vibration, history and other options
 */
import com.royalshield.app.ui.screens.SettingsScreen as AppSettingsScreen
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

class SettingsActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize PreferencesManager if not already initialized
        try {
            PreferencesManager.init(applicationContext)
        } catch (_: Exception) {
            // Already initialized
        }
        
        setContent {
            Royal_shieldTheme {
                AppSettingsScreen(
                    onBackPressed = { finish() },
                    onNavigateToCourses = {
                        startActivity(Intent(this, CourseActivity::class.java))
                    },
                    onNavigateToFundamentals = {
                        startActivity(Intent(this, FundamentalActivity::class.java))
                    },
                    onNavigateToAdvancedSettings = {
                        startActivity(Intent(this, AdvancedSettingsActivity::class.java))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    onNavigateToCourses: () -> Unit = {},
    onNavigateToFundamentals: () -> Unit = {},
    onNavigateToAdvancedSettings: () -> Unit
) {
    var vibrationEnabled by remember { mutableStateOf<Boolean>(PreferencesManager.isVibrationEnabled()) }
    var historyEnabled by remember { mutableStateOf<Boolean>(PreferencesManager.isHistoryEnabled()) }
    
    val gradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
            MaterialTheme.colorScheme.surface
        )
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
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // Transparent for image background
                    titleContentColor = RoyalGold,
                    navigationIconContentColor = RoyalGold
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Emergency Contact
                var emergencyPhone by remember { mutableStateOf(PreferencesManager.getEmergencyPhone() ?: "") }
                var showSaveMessage by remember { mutableStateOf(false) }
                
                SettingsSection(
                    title = "📞 Emergency Contact",
                    description = "Configure the number where the SOS message will be sent"
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Phone Number",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            OutlinedTextField(
                                value = emergencyPhone,
                                onValueChange = { 
                                    emergencyPhone = it
                                    showSaveMessage = false
                                },
                                label = { Text("Ex: +1234567890") },
                                placeholder = { Text("Enter emergency number") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            Button(
                                onClick = {
                                    if (emergencyPhone.isNotBlank()) {
                                        PreferencesManager.saveEmergencyPhone(emergencyPhone.trim())
                                        showSaveMessage = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                enabled = emergencyPhone.isNotBlank()
                            ) {
                                Text("💾 Save contact")
                            }
                            
                            if (showSaveMessage) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "✓ Contact saved successfully",
                                        modifier = Modifier.padding(12.dp),
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Section: Advanced
                SettingsSection(
                    title = "⚙️ Advanced Options",
                    description = "Theme, dark mode, and system permissions"
                ) {
                    Button(
                        onClick = onNavigateToAdvancedSettings,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SettingsSuggest, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Advanced Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                // Section: SOS Button Behavior
                SettingsSection(
                    title = "⚡ SOS Button Behavior",
                    description = "Configure how the app responds when the emergency button is pressed"
                ) {
                    // Vibration switch
                    SettingsSwitchItem(
                        title = "Vibration on SOS activation",
                        description = "The device will vibrate briefly when the panic button is pressed",
                        checked = vibrationEnabled,
                        onCheckedChange = { enabled ->
                            vibrationEnabled = enabled
                            PreferencesManager.setVibrationEnabled(enabled)
                        }
                    )
                }
                
                // Section: Privacy and History
                SettingsSection(
                    title = "🔒 Privacy and History",
                    description = "Manage how your emergency events are stored"
                ) {
                    // History switch
                    SettingsSwitchItem(
                        title = "Save event history",
                        description = "Records date, time and location of each SOS event for later consultation",
                        checked = historyEnabled,
                        onCheckedChange = { enabled ->
                            historyEnabled = enabled
                            PreferencesManager.setHistoryEnabled(enabled)
                        }
                    )
                }
                
                // Section: API Configurations
                SettingsSection(
                    title = "🤖 API Configurations",
                    description = "Manage API Keys for AI and VirusTotal scans"
                ) {
                    var apiKeyOpenAi by remember { mutableStateOf(PreferencesManager.getOpenAiApiKey() ?: "") }
                    var apiKeyVirusTotal by remember { mutableStateOf(PreferencesManager.getVirusTotalApiKey() ?: "") }
                    var showApiSaveMessage by remember { mutableStateOf(false) }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "OpenAI API Key",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedTextField(
                                value = apiKeyOpenAi,
                                onValueChange = { 
                                    apiKeyOpenAi = it
                                    showApiSaveMessage = false 
                                },
                                label = { Text("sk-...") },
                                placeholder = { Text("Enter OpenAI Key") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "VirusTotal API Key",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedTextField(
                                value = apiKeyVirusTotal,
                                onValueChange = { 
                                    apiKeyVirusTotal = it
                                    showApiSaveMessage = false 
                                },
                                label = { Text("API Key") },
                                placeholder = { Text("Enter VirusTotal Key") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    PreferencesManager.saveOpenAiApiKey(apiKeyOpenAi.trim())
                                    PreferencesManager.saveVirusTotalApiKey(apiKeyVirusTotal.trim())
                                    showApiSaveMessage = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("💾 Save API Keys")
                            }

                            if (showApiSaveMessage) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "✓ API Keys updated successfully",
                                        modifier = Modifier.padding(12.dp),
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Section: About Us (Social Media)
                SettingsSection(
                    title = "About Us",
                    description = "Follow us on social media"
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            
                            SocialMediaIcon(
                                iconRes = com.royalshield.app.R.drawable.ic_social_twitter,
                                contentDescription = "Twitter",
                                onClick = { 
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/royalshield"))
                                    context.startActivity(intent)
                                }
                            )
                            SocialMediaIcon(
                                iconRes = com.royalshield.app.R.drawable.ic_social_youtube,
                                contentDescription = "YouTube",
                                onClick = { 
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/@royalshield"))
                                    context.startActivity(intent)
                                }
                            )
                            SocialMediaIcon(
                                iconRes = com.royalshield.app.R.drawable.ic_social_instagram,
                                contentDescription = "Instagram",
                                onClick = { 
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/royalshieldapp"))
                                    context.startActivity(intent)
                                }
                            )
                            SocialMediaIcon(
                                iconRes = com.royalshield.app.R.drawable.ic_social_facebook,
                                contentDescription = "Facebook",
                                onClick = { 
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/profile.php?id=61573434707140"))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }

                // Section: RESOURCE
                SettingsSection(
                    title = "RESOURCE",
                    description = "Help & Legal"
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        
                        Column {
                            SupportItem("❓ Help") { 
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://royalshield.app/help"))
                                context.startActivity(intent)
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                            SupportItem("📄 Terms and Condition") { 
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://royalshield.app/terms"))
                                context.startActivity(intent)
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                            SupportItem("🔐 Privacy") { 
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://royalshield.app/privacy"))
                                context.startActivity(intent)
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                            SupportItem("⭐ Contact and Rate Us") { 
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:support@royalshield.app")
                                    putExtra(Intent.EXTRA_SUBJECT, "Royal Shield - Feedback")
                                }
                                context.startActivity(Intent.createChooser(emailIntent, "Contact Us"))
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                            SupportItem("❤️ Donate") { 
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://royalshield.app/donate"))
                                context.startActivity(intent)
                            }
                        }
                    }
                }

                // Section: Information
                SettingsSection(
                    title = "ℹ️ Information",
                    description = "Application details"
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfoRow(label = "Version", value = "2.0")
                            InfoRow(label = "Developer", value = "Royal Shield Team")
                            InfoRow(label = "Minimum SDK", value = "API 26 (Android 8.0)")
                        }
                    }
                }
                
                // Security note
                SecurityNote()
                
                Spacer(modifier = Modifier.height(32.dp))
            }
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
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
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
            containerColor = MaterialTheme.colorScheme.surface
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
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
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
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "The SOS button opens your messaging app. Make sure you have configured an emergency contact before using it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SocialMediaIcon(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    }
}

@Composable
fun SupportItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        /*
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.btn_forward_gold),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        */
        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(24.dp))
    }
}

