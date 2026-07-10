package com.royalshield.app.featuregates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.subscription.EntitlementStore
import com.royalshield.app.subscription.SubscriptionTier
import com.royalshield.app.ui.theme.RoyalGold

@Composable
fun RequireTier(
    currentTier: SubscriptionTier,
    requiredTier: SubscriptionTier,
    onNavigateToPlans: () -> Unit,
    content: @Composable () -> Unit
) {
    // SUPERPOWERS: Bypass all tier gates
    if (EntitlementStore.DEV_SUPERPOWERS || currentTier.isAtLeast(requiredTier)) {
        content()
    } else {
        LockedUpsellCard(requiredTier, onNavigateToPlans)
    }
}

@Composable
fun LockedUpsellCard(
    requiredTier: SubscriptionTier,
    onNavigateToPlans: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111), RoundedCornerShape(16.dp))
                .border(1.dp, Color.Gray.copy(alpha=0.3f), RoundedCornerShape(16.dp))
                .padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = RoyalGold,
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = "Feature Locked",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "This feature requires the ${requiredTier.name} plan or higher.",
                color = Color.Gray,
                fontSize = 14.sp
            )
            
            Button(
                onClick = onNavigateToPlans,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalGold)
            ) {
                Text("Upgrade to ${requiredTier.name}", color = Color.Black)
            }
        }
    }
}
