package com.royalshield.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.royalshield.app.R

enum class RoyalEliteBadgeMode {
    PROMOTIONAL,
    LOCKED,
    ACTIVE_VERIFIED
}

data class RoyalEliteBadgePresentation(
    val mode: RoyalEliteBadgeMode,
    val statusText: String,
    val contentDescription: String,
    val alpha: Float,
    val showLock: Boolean
)

fun royalEliteBadgePresentation(mode: RoyalEliteBadgeMode): RoyalEliteBadgePresentation = when (mode) {
    RoyalEliteBadgeMode.PROMOTIONAL -> RoyalEliteBadgePresentation(
        mode = mode,
        statusText = "Preview of Royal Elite benefit",
        contentDescription = "Royal Elite benefit preview",
        alpha = 1f,
        showLock = false
    )
    RoyalEliteBadgeMode.LOCKED -> RoyalEliteBadgePresentation(
        mode = mode,
        statusText = "Royal Elite badge locked",
        contentDescription = "Royal Elite badge locked",
        alpha = 0.58f,
        showLock = true
    )
    RoyalEliteBadgeMode.ACTIVE_VERIFIED -> RoyalEliteBadgePresentation(
        mode = mode,
        statusText = "Royal Elite verified active badge",
        contentDescription = "Royal Elite verified active badge",
        alpha = 1f,
        showLock = false
    )
}

fun subscriptionEliteBadgeMode(isPremium: Boolean): RoyalEliteBadgeMode {
    @Suppress("UNUSED_VARIABLE")
    val localPremiumState = isPremium
    return RoyalEliteBadgeMode.PROMOTIONAL
}

@Composable
fun RoyalEliteBadge(
    mode: RoyalEliteBadgeMode,
    modifier: Modifier = Modifier
) {
    val presentation = royalEliteBadgePresentation(mode)

    Box(modifier = modifier.alpha(presentation.alpha)) {
        Image(
            painter = painterResource(R.drawable.royal_elite_badge),
            contentDescription = presentation.contentDescription,
            contentScale = ContentScale.Fit
        )
        if (presentation.showLock) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = presentation.statusText,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
