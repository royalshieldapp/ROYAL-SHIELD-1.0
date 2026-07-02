package com.royalshield.app.core.designsystem

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Dimens {
    // --- BUTTONS CONTRACT ---
    object Buttons {
        // PrimaryButton
        val PrimaryWidth = 320.dp
        val PrimaryHeight = 56.dp
        val PrimaryRadius = 28.dp
        val PrimaryPadding = 16.dp
        val PrimaryIconSize = 24.dp
        
        // SecondaryButton
        val SecondaryWidth = 240.dp
        val SecondaryHeight = 48.dp
        val SecondaryRadius = 24.dp
        val SecondaryPadding = 12.dp
        
        // IconButton
        val IconWidth = 48.dp
        val IconHeight = 48.dp
        val IconContentSize = 28.dp
        val IconPadding = 8.dp
    }

    // --- CARDS CONTRACT ---
    object Cards {
        // FeatureCard (Squareish/Vertical)
        val FeatureWidth = 160.dp
        val FeatureHeight = 200.dp
        val FeatureRadius = 20.dp
        val FeaturePadding = 16.dp
        val FeatureHeroSize = 120.dp // Size for card_<name>_hero image
        
        // StatsCard (Small, landscape)
        val StatsWidth = 154.dp
        val StatsHeight = 120.dp
        val StatsRadius = 16.dp
        val StatsPadding = 12.dp
        val StatsThumbSize = 48.dp // Size for card_<name>_thumb image
        
        // PromoCard (Wide, landscape)
        val PromoWidth = 320.dp
        val PromoHeight = 160.dp
        val PromoRadius = 24.dp
        val PromoPadding = 20.dp
        val PromoHeroSize = 140.dp
    }

    // --- TEXT SIZES ---
    object Typography {
        val TitleLarge = 22.sp
        val TitleMedium = 18.sp
        val BodyLarge = 16.sp
        val BodyMedium = 14.sp
        val Caption = 12.sp
    }

    // --- SPACING ---
    val SpacingSmall = 8.dp
    val SpacingMedium = 16.dp
    val SpacingLarge = 24.dp
    val SpacingExtraLarge = 32.dp
}
