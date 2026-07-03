package com.royalshield.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.managers.ReferralManager
import com.royalshield.app.models.*
import com.royalshield.app.ui.components.ProtectionStatus
import com.royalshield.app.ui.components.ProtectionStatusCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen for managing referral invitations
 * Shows invite limits, active invites, and Duo partner info
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val referralManager = remember { ReferralManager(context) }

    var userPlanData by remember { mutableStateOf<UserPlanData?>(null) }
    var invitesList by remember { mutableStateOf<List<Invite>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isCreatingInvite by remember { mutableStateOf(false) }

    // Load data
    LaunchedEffect(Unit) {
        scope.launch {
            val planResult = referralManager.getCurrentUserPlanData()
            if (planResult.isSuccess) {
                userPlanData = planResult.getOrNull()
            }

            val invitesResult = referralManager.getUserInvites()
            if (invitesResult.isSuccess) {
                invitesList = invitesResult.getOrNull() ?: emptyList()
            }
            
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Referral Program",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFFD700))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Current Plan Status
                item {
                    userPlanData?.let { data ->
                        CurrentPlanCard(data)
                    }
                }

                // Duo Partner Info (if applicable)
                item {
                    userPlanData?.let { data ->
                        if (data.planTier == PlanTier.DUO && data.duoPartnerEmail != null) {
                            DuoPartnerCard(data.duoPartnerEmail!!)
                        }
                    }
                }

                // Invite Limits Card
                item {
                    userPlanData?.let { data ->
                        InviteLimitsCard(data.referral)
                    }
                }

                // Create Invite Button
                item {
                    CreateInviteButton(
                        isCreating = isCreatingInvite,
                        userPlanData = userPlanData,
                        onClick = {
                            isCreatingInvite = true
                            scope.launch {
                                val result = referralManager.createInvite()
                                isCreatingInvite = false

                                if (result.isSuccess) {
                                    val invite = result.getOrNull()!!
                                    invitesList = listOf(invite) + invitesList
                                    
                                    // Update user data
                                    val updatedPlan = referralManager.getCurrentUserPlanData()
                                    if (updatedPlan.isSuccess) {
                                        userPlanData = updatedPlan.getOrNull()
                                    }

                                    Toast.makeText(
                                        context,
                                        "✅ Invite created: ${invite.code}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "❌ ${result.exceptionOrNull()?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    )
                }

                // Invites List
                item {
                    Text(
                        text = "YOUR INVITES (${invitesList.size})",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (invitesList.isEmpty()) {
                    item {
                        EmptyInvitesPlaceholder()
                    }
                } else {
                    items(invitesList) { invite ->
                        InviteCard(
                            invite = invite,
                            onShare = {
                                val link = referralManager.generateInviteLink(invite.code)
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "🛡️ Join me on Royal Shield with this exclusive invite!\n\n" +
                                        "Code: ${invite.code}\n" +
                                        "Link: $link\n\n" +
                                        "Get 2-for-1 premium protection when you sign up!"
                                    )
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share Invite")
                                )
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun CurrentPlanCard(data: UserPlanData) {
    val status = if (data.planTier == PlanTier.DUO) {
        ProtectionStatus.PREMIUM
    } else {
        ProtectionStatus.SAFE
    }

    ProtectionStatusCard(
        status = status,
        title = data.planTier.name,
        subtitle = "Current Plan",
        description = when (data.planTier) {
            PlanTier.FREE -> "Upgrade to unlock referrals"
            PlanTier.SOLO -> "Solo protection active"
            PlanTier.DUO -> "2-for-1 Protection with partner"
        },
        customIcon = when (data.planTier) {
            PlanTier.FREE -> Icons.Default.Lock
            PlanTier.SOLO -> Icons.Default.Person
            PlanTier.DUO -> Icons.Default.Group
        }
    )
}

@Composable
private fun DuoPartnerCard(partnerEmail: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A1A2E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Duo Partner",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
                Text(
                    text = partnerEmail,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun InviteLimitsCard(referral: ReferralData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF121216)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "INVITE LIMITS",
                color = Color(0xFFFFD700),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LimitIndicator(
                    label = "Active Invites",
                    current = referral.activeInviteCount,
                    max = ReferralLimits.MAX_ACTIVE_INVITES
                )

                LimitIndicator(
                    label = "This Month",
                    current = if (referral.monthKey == getCurrentMonthKey()) {
                        referral.monthlyInviteCount
                    } else {
                        0
                    },
                    max = ReferralLimits.MAX_INVITES_PER_MONTH
                )
            }
        }
    }
}

@Composable
private fun LimitIndicator(label: String, current: Int, max: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$current / $max",
            color = if (current >= max) Color(0xFFFF3131) else Color(0xFF00FF94),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun CreateInviteButton(
    isCreating: Boolean,
    userPlanData: UserPlanData?,
    onClick: () -> Unit
) {
    val canCreate = userPlanData?.let { data ->
        data.referral.activeInviteCount < ReferralLimits.MAX_ACTIVE_INVITES &&
        (data.referral.monthKey != getCurrentMonthKey() || 
         data.referral.monthlyInviteCount < ReferralLimits.MAX_INVITES_PER_MONTH)
    } ?: false

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = canCreate && !isCreating,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFD700),
            contentColor = Color.Black,
            disabledContainerColor = Color(0xFF3A3A3A),
            disabledContentColor = Color(0xFF64748B)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isCreating) {
            CircularProgressIndicator(
                color = Color.Black,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (canCreate) "CREATE NEW INVITE" else "LIMIT REACHED",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun InviteCard(invite: Invite, onShare: () -> Unit) {
    val statusColor = when (invite.status) {
        InviteStatus.PENDING -> Color(0xFFFFD700)
        InviteStatus.CLAIMED -> Color(0xFF00BFFF)
        InviteStatus.ACTIVATED -> Color(0xFF00FF94)
        InviteStatus.EXPIRED -> Color(0xFF64748B)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1A1A2E),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = invite.code,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = invite.status.name,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details
            InviteDetailRow("Created", formatTimestamp(invite.createdAt))
            InviteDetailRow("Expires", formatTimestamp(invite.expiresAt))

            if (invite.claimedByEmail != null) {
                InviteDetailRow("Claimed by", invite.claimedByEmail)
            }

            // Share button (only for pending invites)
            if (invite.status == InviteStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFFD700)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFFFFD700)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SHARE INVITE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InviteDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF64748B), fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmptyInvitesPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "No invites yet",
                color = Color(0xFF64748B),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Create your first invite to share\nRoyal Shield with friends",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getCurrentMonthKey(): String {
    val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
    return sdf.format(Date())
}
