package com.royalshield.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.SafeGreen

data class ComplianceStandard(
    val id: String,
    val name: String,
    val description: String,
    val status: ComplianceStatus,
    val coverage: Int, // 0-100%
    val requirements: List<ComplianceRequirement>
)

data class ComplianceRequirement(
    val title: String,
    val isMet: Boolean,
    val description: String
)

enum class ComplianceStatus {
    COMPLIANT, PARTIAL, NON_COMPLIANT, NOT_APPLICABLE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplianceMonitorScreen(onBack: () -> Unit) {
    val standards = remember { getComplianceStandards() }
    var selectedStandard by remember { mutableStateOf<ComplianceStandard?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compliance Monitor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.btn_back_gold),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = RoyalGold,
                    navigationIconContentColor = RoyalGold
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Overall Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Overall Compliance", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("3 of 4 standards met", color = Color.Gray, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier.size(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { 0.75f },
                            modifier = Modifier.fillMaxSize(),
                            color = SafeGreen,
                            strokeWidth = 6.dp,
                            trackColor = Color.Gray.copy(alpha = 0.3f)
                        )
                        Text("75%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Compliance Standards", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(standards) { standard ->
                    ComplianceStandardCard(
                        standard = standard,
                        onClick = { selectedStandard = standard }
                    )
                }
            }
        }
    }
    
    // Details Dialog
    selectedStandard?.let { standard ->
        AlertDialog(
            onDismissRequest = { selectedStandard = null },
            containerColor = Color(0xFF1E1E1E),
            title = { 
                Column {
                    Text(standard.name, color = RoyalGold, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(standard.description, color = Color.Gray, fontSize = 12.sp)
                }
            },
            text = {
                Column {
                    Text("Requirements Checklist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    standard.requirements.forEach { req ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                if (req.isMet) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (req.isMet) SafeGreen else Color(0xFFFF5252),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(req.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(req.description, color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedStandard = null }) {
                    Text("Close", color = RoyalGold)
                }
            }
        )
    }
}

@Composable
fun ComplianceStandardCard(
    standard: ComplianceStandard,
    onClick: () -> Unit
) {
    val statusColor = when (standard.status) {
        ComplianceStatus.COMPLIANT -> SafeGreen
        ComplianceStatus.PARTIAL -> Color(0xFFFF9800)
        ComplianceStatus.NON_COMPLIANT -> Color(0xFFFF5252)
        ComplianceStatus.NOT_APPLICABLE -> Color.Gray
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        when (standard.status) {
                            ComplianceStatus.COMPLIANT -> Icons.Default.CheckCircle
                            ComplianceStatus.PARTIAL -> Icons.Default.Warning
                            ComplianceStatus.NON_COMPLIANT -> Icons.Default.Error
                            ComplianceStatus.NOT_APPLICABLE -> Icons.Default.RemoveCircle
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(standard.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(standard.description, color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress Bar
                LinearProgressIndicator(
                    progress = { standard.coverage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = statusColor,
                    trackColor = Color.Gray.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("${standard.coverage}% compliant", color = Color.Gray, fontSize = 10.sp)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun getComplianceStandards(): List<ComplianceStandard> {
    return listOf(
        ComplianceStandard(
            id = "gdpr",
            name = "GDPR",
            description = "General Data Protection Regulation (EU)",
            status = ComplianceStatus.COMPLIANT,
            coverage = 100,
            requirements = listOf(
                ComplianceRequirement("Data Encryption", true, "All sensitive data is encrypted at rest"),
                ComplianceRequirement("User Consent", true, "Explicit consent mechanisms in place"),
                ComplianceRequirement("Data Portability", true, "Users can export their data"),
                ComplianceRequirement("Right to Deletion", true, "Data deletion on user request")
            )
        ),
        ComplianceStandard(
            id = "hipaa",
            name = "HIPAA",
            description = "Health Insurance Portability and Accountability Act (US)",
            status = ComplianceStatus.PARTIAL,
            coverage = 75,
            requirements = listOf(
                ComplianceRequirement("Access Controls", true, "Role-based access implemented"),
                ComplianceRequirement("Audit Logging", true, "All access is logged"),
                ComplianceRequirement("Encryption", true, "PHI data encrypted"),
                ComplianceRequirement("Business Associate Agreements", false, "BAAs not yet signed")
            )
        ),
        ComplianceStandard(
            id = "iso27001",
            name = "ISO 27001",
            description = "Information Security Management",
            status = ComplianceStatus.COMPLIANT,
            coverage = 95,
            requirements = listOf(
                ComplianceRequirement("Risk Assessment", true, "Annual risk assessments conducted"),
                ComplianceRequirement("Security Policies", true, "Comprehensive policies in place"),
                ComplianceRequirement("Incident Response", true, "IRP documented and tested"),
                ComplianceRequirement("Training", true, "Regular security awareness training")
            )
        ),
        ComplianceStandard(
            id = "pci-dss",
            name = "PCI-DSS",
            description = "Payment Card Industry Data Security Standard",
            status = ComplianceStatus.NOT_APPLICABLE,
            coverage = 0,
            requirements = listOf(
                ComplianceRequirement("N/A", false, "No payment card data processed")
            )
        )
    )
}
