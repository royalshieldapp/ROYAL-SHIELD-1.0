package com.royalshield.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    // Ads disabled by user request
    Spacer(modifier = modifier.size(0.dp))
}

