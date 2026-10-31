package com.koussa.islamicazkar.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Apple's system green, as used by iOS switches. */
fun appleGreen(dark: Boolean) = if (dark) Color(0xFF30D158) else Color(0xFF34C759)

/** An iOS-style switch: 51×31 capsule with a springy white knob. */
@Composable
fun IosSwitch(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val g = Glass.colors
    val track by animateColorAsState(
        if (checked) appleGreen(g.dark) else if (g.dark) Color(0x52787880) else Color(0x29787880),
        label = "track",
    )
    val x by animateDpAsState(if (checked) 20.dp else 0.dp, spring(dampingRatio = 0.62f, stiffness = 520f), label = "knob")
    Box(
        modifier
            .size(51.dp, 31.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
            .pressable({ onChange(!checked) })
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = x)
                .size(27.dp)
                .shadow(3.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.25f))
                .background(Color.White, CircleShape),
        )
    }
}
