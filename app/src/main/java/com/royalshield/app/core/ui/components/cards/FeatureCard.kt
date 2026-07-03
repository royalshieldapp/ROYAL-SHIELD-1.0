package com.royalshield.app.core.ui.components.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.royalshield.app.core.designsystem.Dimens

@Composable
fun FeatureCard(
    title: String,
    heroIconRes: Int, // Example: R.drawable.card_malware_hero
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(Dimens.Cards.FeatureWidth)
            .height(Dimens.Cards.FeatureHeight)
            .clip(RoundedCornerShape(Dimens.Cards.FeatureRadius))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.Cards.FeaturePadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(id = heroIconRes),
                contentDescription = null,
                modifier = Modifier.size(Dimens.Cards.FeatureHeroSize),
                contentScale = ContentScale.Fit
            )
            
            Text(
                text = title.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = com.royalshield.app.core.designsystem.Dimens.Typography.BodyMedium
            )
        }
    }
}
