package com.royalshield.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.ui.theme.RoyalGold

/**
 * Business Dashboard - Herramientas empresariales funcionales
 * Cada tarjeta navega a una funcionalidad REAL implementada
 */

@Composable
fun BusinessDashboardScreen(
    onBack: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToCompliance: () -> Unit,
    onNavigateToAuditLogs: () -> Unit,
    onNavigateToPolicies: () -> Unit,
    onNavigateToIrHub: () -> Unit,
    onNavigateToIntelHub: () -> Unit
) {
    val gold = Color(0xFFFFC107)

    Box(modifier = Modifier.fillMaxSize()) {
        // Updated Background Image
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.bg_business_tool),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Dark Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = RoyalGold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Business Tools",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Enterprise Security Management",
                        fontSize = 12.sp,
                        color = gold
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A).copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, gold.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Business, null, tint = gold, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Welcome to Business Suite", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("Manage your organization's security", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Text(
                    "BUSINESS TOOLS",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Tool 1: Reports Center
                BusinessToolCard(
                    title = "Reports Center",
                    description = "Generate PDF/CSV security reports",
                    imageRes = com.royalshield.app.R.drawable.card_business_bg,
                    status = "Active",
                    statusColor = Color(0xFF4CAF50),
                    onClick = onNavigateToReports
                )

                // Tool 2: Compliance Monitor
                BusinessToolCard(
                    title = "Compliance Monitor",
                    description = "GDPR, HIPAA, ISO 27001 compliance",
                    imageRes = com.royalshield.app.R.drawable.course_bg_business,
                    status = "Monitoring",
                    statusColor = Color(0xFF2196F3),
                    onClick = onNavigateToCompliance
                )

                // Tool 3: Audit Logs
                BusinessToolCard(
                    title = "Audit Logs & Events",
                    description = "Security event tracking & forensics",
                    imageRes = com.royalshield.app.R.drawable.ic_threat_briefing_gold,
                    status = "Recording",
                    statusColor = gold,
                    onClick = onNavigateToAuditLogs
                )

                // Tool 4: Policy Manager
                BusinessToolCard(
                    title = "Policy Manager",
                    description = "Centralized device & user policies",
                    imageRes = com.royalshield.app.R.drawable.bg_map_cyber,
                    status = "12 Active",
                    statusColor = Color(0xFFFF9800),
                    onClick = onNavigateToPolicies
                )

                // Tool 5: Incident Response (PHASE 4)
                BusinessToolCard(
                    title = "Incident Response (IR)",
                    description = "Execute playbooks & manage breaches",
                    imageRes = com.royalshield.app.R.drawable.dashboard_card_malware,
                    status = "Ready",
                    statusColor = Color(0xFFFF1744),
                    onClick = onNavigateToIrHub
                )

                // Tool 6: Threat Radar (PHASE 4)
                BusinessToolCard(
                    title = "Threat Radar Hub",
                    description = "Dark web monitoring & IOCs",
                    imageRes = com.royalshield.app.R.drawable.bg_business_tool,
                    status = "Live",
                    statusColor = Color(0xFF00E5FF),
                    onClick = onNavigateToIntelHub
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Overview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A).copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Quick Stats", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem("Reports", "24", Icons.Default.Description)
                            StatItem("Policies", "12", Icons.Default.Policy)
                            StatItem("Events", "1.2k", Icons.Default.Event)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BusinessToolCard(
    title: String,
    description: String,
    imageRes: Int,
    status: String,
    statusColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp) // Taller card to show image
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = imageRes),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Dark Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.4f)
                            )
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
               Row(verticalAlignment = Alignment.CenterVertically) {
                   Text(
                       text = title,
                       color = Color.White,
                       fontWeight = FontWeight.Bold,
                       fontSize = 20.sp
                   )
                   Spacer(modifier = Modifier.weight(1f))
                   
                   // Status Badge
                   Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = status,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
               }
               
               Spacer(modifier = Modifier.height(8.dp))
               
               Text(
                   text = description,
                   color = Color(0xFFDDDDDD),
                   fontSize = 13.sp,
                   maxLines = 2
               )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}
