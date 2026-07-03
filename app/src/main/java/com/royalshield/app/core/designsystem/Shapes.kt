package com.royalshield.app.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RoyalShapes = Shapes(
    small = RoundedCornerShape(Dimens.Buttons.SecondaryRadius),
    medium = RoundedCornerShape(Dimens.Cards.StatsRadius),
    large = RoundedCornerShape(Dimens.Cards.PromoRadius),
    extraLarge = RoundedCornerShape(Dimens.Buttons.PrimaryRadius)
)
