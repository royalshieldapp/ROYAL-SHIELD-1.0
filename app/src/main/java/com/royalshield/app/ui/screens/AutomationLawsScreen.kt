package com.royalshield.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.data.db.AutomationRule

@Composable
fun AutomationLawsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E3A8A)) // Blue background from screenshot
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                 IconButton(onClick = onBack) {
                     Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                 }
                 Spacer(modifier = Modifier.width(8.dp))
                 Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFFFD700))
                 Spacer(modifier = Modifier.width(8.dp))
                 Text(
                     "Automation Laws",
                     fontSize = 20.sp,
                     fontWeight = FontWeight.Bold,
                     color = Color.White
                 )
            }

            // Main Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🤖", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Automation\nSystem",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A),
                            lineHeight = 24.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Create smart rules that run automatically based on events, location, schedules, and more.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("0", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                            Text("Total Laws", fontSize = 12.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("0", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF43A047))
                            Text("Active", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "📋 Suggested Laws",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    LawSuggestionCard(
                        icon = Icons.Default.Nightlight,
                        title = "Automatic Night Mode",
                        description = "Activate night protection from 10 PM to 6 AM",
                        color = Color(0xFFFFB74D),
                        onClick = { Toast.makeText(context, "Creating Rule...", Toast.LENGTH_SHORT).show() }
                    )
                }
                item {
                    LawSuggestionCard(
                        icon = Icons.Default.Home,
                        title = "Turn off alarm when arriving home",
                        description = "Deactivates the alarm when you arrive at your home location",
                        color = Color(0xFFE57373),
                        onClick = { Toast.makeText(context, "Creating Rule...", Toast.LENGTH_SHORT).show() }
                    )
                }
                item {
                    LawSuggestionCard(
                        icon = Icons.Default.BatteryAlert,
                        title = "Low Battery Protocol",
                        description = "Automatically sends location when battery < 15%",
                        color = Color(0xFF81C784),
                        onClick = { Toast.makeText(context, "Creating Rule...", Toast.LENGTH_SHORT).show() }
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Wifi, null, tint = Color.Blue, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Network", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Test Speed
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { Toast.makeText(context, "Testing Speed...", Toast.LENGTH_SHORT).show() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bolt, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Test Speed", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Calculate download and upload speed", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha=0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // WiFi Scanner
                             Row(
                                modifier = Modifier.fillMaxWidth().clickable { Toast.makeText(context, "Scanning WiFi...", Toast.LENGTH_SHORT).show() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("WiFi Scanner", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Scan nearby APs for collisions", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LawSuggestionCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Create", fontSize = 12.sp)
            }
        }
    }
}
