package com.royalshield.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.royalshield.app.ui.components.RoyalSecurityOrb
import com.royalshield.app.ui.components.RoyalSecurityOrbState
import com.royalshield.app.ui.theme.RoyalGold

data class DashboardAgentControlPresentation(
    val label: String,
    val requiresConfirmation: Boolean,
)

fun dashboardAgentControlPresentation(armed: Boolean) = DashboardAgentControlPresentation(
    label = if (armed) "SYSTEM ARMED" else "SYSTEM DISARMED",
    requiresConfirmation = armed,
)

@Composable
fun DashboardAgentCenter(
    armed: Boolean,
    onSetArmed: (Boolean) -> Unit,
    onVoiceClick: () -> Unit,
    onChatClick: () -> Unit,
    onSetupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDisarm by remember { mutableStateOf(false) }
    val presentation = dashboardAgentControlPresentation(armed)
    val statusColor = if (armed) Color(0xFF00FF94) else Color(0xFFFF3B30)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "ROYAL SECURITY AGENT",
            color = RoyalGold,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(10.dp))
        RoyalSecurityOrb(
            state = RoyalSecurityOrbState.ACTIVE,
            modifier = Modifier.size(148.dp),
            onClick = onVoiceClick,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = {
                    if (presentation.requiresConfirmation) confirmDisarm = true
                    else onSetArmed(true)
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = statusColor.copy(alpha = 0.18f),
                    contentColor = statusColor,
                ),
                border = BorderStroke(1.dp, statusColor),
            ) {
                Text(presentation.label, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onChatClick,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, RoyalGold),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalGold),
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(19.dp))
                Text("  CHAT", fontWeight = FontWeight.Bold)
            }
        }
        TextButton(onClick = onSetupClick) {
            Text("SECURITY SETUP", color = RoyalGold, fontWeight = FontWeight.Bold)
        }
    }

    if (confirmDisarm) {
        AlertDialog(
            onDismissRequest = { confirmDisarm = false },
            title = { Text("Disarm protection?") },
            text = { Text("This stops Safe Path monitoring. Independent protection modules keep their own current state.") },
            confirmButton = {
                TextButton(onClick = {
                    onSetArmed(false)
                    confirmDisarm = false
                }) { Text("DISARM", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisarm = false }) { Text("CANCEL", color = RoyalGold) }
            },
            containerColor = Color(0xFF1E1E1E),
            textContentColor = Color.White,
        )
    }
}
