package com.royalshield.app.core.ui.components.buttons

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.royalshield.app.core.designsystem.Dimens

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null // Example: R.drawable.btn_scan_icon
) {
    Box(
        modifier = modifier
            .width(Dimens.Buttons.PrimaryWidth)
            .height(Dimens.Buttons.PrimaryHeight)
            .clip(RoundedCornerShape(Dimens.Buttons.PrimaryRadius))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFFFC107), Color(0xFFFFA000)) // Gold Neon Gradient
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = Dimens.Buttons.PrimaryPadding)
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.Buttons.PrimaryIconSize)
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
            }
            Text(
                text = text.uppercase(),
                color = Color.Black,
                fontWeight = FontWeight.Black,
                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
            )
        }
    }
}
