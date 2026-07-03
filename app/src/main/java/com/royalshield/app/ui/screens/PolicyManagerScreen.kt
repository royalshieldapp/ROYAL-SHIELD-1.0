package com.royalshield.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.ui.theme.RoyalGold
import com.royalshield.app.ui.theme.SafeGreen
import java.util.UUID

data class SecurityPolicy(
    val id: String,
    val name: String,
    val description: String,
    val scope: PolicyScope,
    val isEnabled: Boolean,
    val affectedDevices: Int,
    val rules: List<PolicyRule>
)

data class PolicyRule(
    val condition: String,
    val action: String
)

enum class PolicyScope {
    ALL_DEVICES, SPECIFIC_USERS, SPECIFIC_DEVICES, OFFICE_NETWORK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyManagerScreen(onBack: () -> Unit) {
    var policies by remember { mutableStateOf(getSamplePolicies()) }
    var selectedPolicy by remember { mutableStateOf<SecurityPolicy?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Policy Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.royalshield.app.R.drawable.btn_back_gold),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Sync policies */ }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync", tint = RoyalGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = RoyalGold,
                    navigationIconContentColor = RoyalGold
                )
            )
        },
        containerColor = Color.Black,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = RoyalGold,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Policy")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PolicyStat("Active", policies.count { it.isEnabled }.toString(), Icons.Default.CheckCircle)
                    PolicyStat("Inactive", policies.count { !it.isEnabled }.toString(), Icons.Default.Cancel)
                    PolicyStat("Devices", policies.sumOf { it.affectedDevices }.toString(), Icons.Default.Devices)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Active Policies", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("${policies.size} policies configured", color = Color.Gray, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(policies) { policy ->
                    PolicyCard(
                        policy = policy,
                        onToggle = { enabled ->
                            policies = policies.map {
                                if (it.id == policy.id) it.copy(isEnabled = enabled) else it
                            }
                        },
                        onClick = { selectedPolicy = policy }
                    )
                }
            }
        }
    }
    
    // Policy Details Dialog
    selectedPolicy?.let { policy ->
        AlertDialog(
            onDismissRequest = { selectedPolicy = null },
            containerColor = Color(0xFF1E1E1E),
            title = {
                Column {
                    Text(policy.name, color = RoyalGold, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(policy.description, color = Color.Gray, fontSize = 12.sp)
                }
            },
            text = {
                Column {
                    Text("Scope: ${policy.scope.name.replace("_", " ")}", color = Color.White, fontSize = 13.sp)
                    Text("Affected Devices: ${policy.affectedDevices}", color = Color.White, fontSize = 13.sp)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Policy Rules", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    policy.rules.forEach { rule ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ArrowForward, null, tint = RoyalGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("IF: ${rule.condition}", color = Color.White, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, null, tint = SafeGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("THEN: ${rule.action}", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPolicy = null }) {
                    Text("Close", color = RoyalGold)
                }
            }
        )
    }
    
    // Create Policy Dialog
    if (showCreateDialog) {
        var policyName by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("Create New Policy", color = RoyalGold, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Policy Name", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = policyName,
                        onValueChange = { policyName = it },
                        placeholder = { Text("e.g., Block Social Media", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RoyalGold,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Common Templates:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    PolicyTemplate("Block Social Media", "During work hours") { policyName = "Block Social Media" }
                    PolicyTemplate("Enable VPN", "On public WiFi") { policyName = "Enable VPN" }
                    PolicyTemplate("Require MFA", "For sensitive apps") { policyName = "Require MFA" }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (policyName.isNotBlank()) {
                            val newPolicy = SecurityPolicy(
                                UUID.randomUUID().toString(),
                                policyName,
                                "Custom policy created by admin",
                                PolicyScope.ALL_DEVICES,
                                true,
                                0,
                                emptyList()
                            )
                            policies = policies + newPolicy
                            showCreateDialog= false
                            policyName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalGold)
                ) {
                    Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun PolicyStat(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = RoyalGold, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
fun PolicyCard(
    policy: SecurityPolicy,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
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
                color = if (policy.isEnabled) SafeGreen.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Policy,
                        contentDescription = null,
                        tint = if (policy.isEnabled) SafeGreen else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(policy.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(policy.description, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Devices, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${policy.affectedDevices} devices", color = Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Default.Rule, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${policy.rules.size} rules", color = Color.Gray, fontSize = 11.sp)
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Switch(
                checked = policy.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SafeGreen,
                    checkedTrackColor = SafeGreen.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun PolicyTemplate(title: String, description: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AddCircle, null, tint = RoyalGold, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(description, color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

fun getSamplePolicies(): List<SecurityPolicy> {
    return listOf(
        SecurityPolicy(
            "1",
            "Block Social Media at Work",
            "Blocks Facebook, Instagram, Twitter during 9-5",
            PolicyScope.OFFICE_NETWORK,
            true,
            24,
            listOf(
                PolicyRule("Time is 09:00-17:00 AND Network is Office", "Block social media domains"),
                PolicyRule("User attempts to access blocked site", "Show warning and log event")
            )
        ),
        SecurityPolicy(
            "2",
            "Auto-Enable VPN on Public WiFi",
            "Automatically connects VPN when detecting public networks",
            PolicyScope.ALL_DEVICES,
            true,
            45,
            listOf(
                PolicyRule("WiFi is public/unsecured", "Enable VPN connection"),
                PolicyRule("VPN connection fails", "Block internet access and alert user")
            )
        ),
        SecurityPolicy(
            "3",
            "Enforce MFA for Finance Apps",
            "Requires multi-factor authentication for banking and finance apps",
            PolicyScope.SPECIFIC_USERS,
            true,
            15,
            listOf(
                PolicyRule("User opens finance app", "Require biometric authentication"),
                PolicyRule("3 failed auth attempts", "Lock app for 30 minutes")
            )
        ),
        SecurityPolicy(
            "4",
            "Data Loss Prevention",
            "Prevents copying sensitive data to clipboard or external apps",
            PolicyScope.ALL_DEVICES,
            false,
            45,
            listOf(
                PolicyRule("Sensitive data detected in clipboard", "Clear clipboard and alert"),
                PolicyRule("Attempt to share file externally", "Require admin approval")
            )
        )
    )
}
