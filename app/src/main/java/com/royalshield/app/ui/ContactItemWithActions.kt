package com.royalshield.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.royalshield.app.Contact
import com.royalshield.app.ui.theme.RoyalTheme

@Composable
fun ContactItemWithActions(
    contact: Contact,
    onSend: (Contact) -> Unit,
    onDelete: (Contact) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RoyalTheme.Surface, RoyalTheme.ButtonShape)
            .border(1.dp, RoyalTheme.SurfaceHighlight, RoyalTheme.ButtonShape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(contact.name, color = RoyalTheme.TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(contact.phone, color = RoyalTheme.TextSecondary, fontSize = 12.sp)
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = RoyalTheme.AccentRed,
                shape = CircleShape,
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onSend(contact) },
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Eliminar",
                tint = RoyalTheme.TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onDelete(contact) }
            )
        }
    }
}
