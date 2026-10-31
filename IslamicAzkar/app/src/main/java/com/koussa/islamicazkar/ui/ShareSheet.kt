package com.koussa.islamicazkar.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Zikr
import com.koussa.islamicazkar.share.ShareCard
import com.koussa.islamicazkar.ui.glass.AccentButton
import com.koussa.islamicazkar.ui.glass.Glass
import com.koussa.islamicazkar.ui.glass.glass
import com.koussa.islamicazkar.ui.glass.pressable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Bottom glass sheet that previews and shares a dhikr as a designed image. */
@Composable
fun ShareSheet(z: Zikr, title: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val g = Glass.colors
    val s = Glass.s
    val lang = Glass.lang
    var style by remember { mutableIntStateOf(0) }
    var withMeaning by remember { mutableStateOf(lang != Lang.AR) }
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    val rise = remember { Animatable(1f) }

    LaunchedEffect(Unit) { rise.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 320f)) }
    LaunchedEffect(style, withMeaning, lang) {
        bmp = withContext(Dispatchers.Default) { ShareCard.render(ctx, z, title, style, lang, withMeaning) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind { drawRect(Color.Black.copy(alpha = 0.38f * (1f - rise.value))) }
            .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .graphicsLayer { translationY = size.height * rise.value }
                .clickable(remember { MutableInteractionSource() }, null) { }
                .navigationBarsPadding()
                .padding(10.dp)
                .glass(RoundedCornerShape(36.dp), strong = true, blurRadius = 36.dp, elevation = 24.dp)
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.width(40.dp).height(5.dp).clip(RoundedCornerShape(50)).drawBehind { drawRect(g.text3) })
            Text(s.shareImage, color = g.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.padding(top = 12.dp, bottom = 14.dp))

            Box(
                Modifier.fillMaxWidth(0.62f).aspectRatio(1080f / 1350f).clip(RoundedCornerShape(20.dp))
                    .border(1.dp, g.rimLight, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                val b = bmp
                if (b == null) CircularProgressIndicator(color = g.accent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                else Image(b.asImageBitmap(), null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                ShareCard.styles.forEachIndexed { i, st ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.pressable({ style = i })) {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape)
                                .drawBehind { drawRect(Brush.linearGradient(listOf(Color(st.bg), Color(st.cardTop)))) }
                                .border(if (i == style) 2.5.dp else 1.dp, if (i == style) g.accent else g.rimLight, CircleShape),
                        )
                        Text(s.styleNames[i], fontSize = 11.sp, color = if (i == style) g.accent else g.text2, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            if (lang != Lang.AR) {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.includeMeaning, color = g.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    com.koussa.islamicazkar.ui.glass.IosSwitch(withMeaning, { withMeaning = it })
                }
            }

            Spacer(Modifier.height(14.dp))
            AccentButton(s.shareNow, Modifier.fillMaxWidth(0.7f), icon = Icons.Rounded.IosShare) {
                bmp?.let { ShareCard.share(ctx, it, "${s.appName} — $title") }
            }
        }
    }
}
