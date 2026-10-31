package com.koussa.islamicazkar.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Reading
import com.koussa.islamicazkar.i18n.COMPLETION_PRAYER
import com.koussa.islamicazkar.i18n.ENCOURAGEMENTS
import com.koussa.islamicazkar.ui.glass.AccentButton
import com.koussa.islamicazkar.ui.glass.ArabicFont
import com.koussa.islamicazkar.ui.glass.Glass
import com.koussa.islamicazkar.ui.glass.glass
import com.koussa.islamicazkar.ui.glass.starPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/** What the reader just completed, for the congratulation card. */
data class Celebration(
    val reading: Reading,
    val title: String,
    val azkar: Int,
    val repetitions: Int,
    val streak: Int?,
    val encouragement: Int,
)

private val Gold = Color(0xFFD6B36A)

/**
 * Shown when every dhikr of a reading is completed: a Liquid Glass card that congratulates
 * the reader, offers them a prayer and an encouraging verse or hadith, and shows their streak.
 */
@Composable
fun CelebrationCard(c: Celebration, onDismiss: () -> Unit) {
    val g = Glass.colors
    val s = Glass.s
    val lang = Glass.lang
    val view = LocalView.current
    val enc = ENCOURAGEMENTS[c.encouragement % ENCOURAGEMENTS.size]
    val haptics = LocalApp.current.settings.haptics

    val scrim = remember { Animatable(0f) }
    val rise = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (haptics) view.performHapticFeedback(if (android.os.Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS)
        launch { scrim.animateTo(1f, tween(250)) }
        launch { rise.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 260f)) }
        delay(150)
        ring.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        launch { check.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 420f)) }
        burst.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f * scrim.value))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .systemBarsPadding()
                .padding(20.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = rise.value.coerceIn(0f, 1f)
                    val sc = 0.9f + 0.1f * rise.value
                    scaleX = sc; scaleY = sc
                }
                .clickable(remember { MutableInteractionSource() }, null) { }
                .glass(
                    RoundedCornerShape(36.dp), blurRadius = 30.dp, elevation = 30.dp,
                    tint = if (g.dark) Color(0xE01C1C1E) else Color(0xE6FFFFFF),
                    fallback = if (g.dark) Color(0xFA1C1C1E) else Color(0xFAFFFFFF),
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Badge(ring.value, check.value, burst.value)

            Text(
                s.congratsTitle, color = g.text, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp),
            )
            Text(s.congratsDone(c.title), color = g.text2, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))

            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Capsule(s.azkar(c.azkar))
                Capsule(s.repetitions(c.repetitions))
            }
            c.streak?.let { n ->
                Row(
                    Modifier.padding(top = 8.dp)
                        .background(Gold.copy(alpha = 0.16f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Gold, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s.streak(n), color = if (g.dark) Gold else Color(0xFF8A6A25), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Ornament(Modifier.padding(vertical = 18.dp))

            Text(s.prayerLabel, color = g.text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = if (lang == Lang.AR) 0.sp else 1.sp)
            Text(
                COMPLETION_PRAYER, fontFamily = ArabicFont, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 38.sp,
                color = g.accent, textAlign = TextAlign.Center, style = TextStyle(textDirection = TextDirection.Rtl),
                modifier = Modifier.padding(top = 6.dp),
            )
            if (s.prayerMeaning.isNotEmpty()) {
                Text(s.prayerMeaning, color = g.text, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }

            Column(
                Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .background(g.fillGray, RoundedCornerShape(20.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    enc.ar, fontFamily = ArabicFont, fontSize = 18.sp, lineHeight = 32.sp, color = g.text,
                    textAlign = TextAlign.Center, style = TextStyle(textDirection = TextDirection.Rtl),
                )
                val m = enc.meaning(lang)
                if (m.isNotEmpty()) Text(m, color = g.text2, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                Text(enc.source(lang), color = g.text3, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            val next = when (c.reading) {
                Reading.Morning -> s.seeYouEvening
                Reading.Evening -> s.seeYouMorning
                else -> null
            }
            next?.let { Text(it, color = g.text2, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp)) }

            Spacer(Modifier.height(18.dp))
            AccentButton(s.alhamdulillah, Modifier.fillMaxWidth(), onClick = onDismiss)
        }
    }
}

/** A ring that fills, a check that pops in, and a few gold stars drifting outward. */
@Composable
private fun Badge(ring: Float, check: Float, burst: Float) {
    val g = Glass.colors
    Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(132.dp).alpha(1f - burst * 0.85f)) {
            val c = center
            for (i in 0 until 8) {
                val a = Math.PI / 4 * i + Math.PI / 8
                val d = size.minDimension * (0.30f + 0.22f * burst)
                val p = Offset(c.x + (d * cos(a)).toFloat(), c.y + (d * sin(a)).toFloat())
                drawPath(starPath(p, size.minDimension * (0.035f + 0.02f * (i % 2)), 0.5f), Gold)
            }
        }
        Canvas(Modifier.size(84.dp)) {
            val sw = 6.dp.toPx()
            val inset = sw / 2
            val arc = Size(size.width - sw, size.height - sw)
            drawCircle(g.accent.copy(alpha = 0.12f))
            drawArc(g.accent.copy(alpha = 0.18f), 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(sw))
            drawArc(
                Brush.sweepGradient(listOf(g.accent, Gold, g.accent)), -90f, 360f * ring, false,
                Offset(inset, inset), arc, style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        Canvas(Modifier.size(34.dp).scale(check).drawBehind { }) {
            val w = size.width
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.12f, w * 0.52f)
                lineTo(w * 0.40f, w * 0.78f)
                lineTo(w * 0.88f, w * 0.24f)
            }
            drawPath(path, g.accent, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}

@Composable
private fun Ornament(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(70.dp).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Gold))))
        Canvas(Modifier.padding(horizontal = 8.dp).size(9.dp)) { drawPath(starPath(center, size.minDimension / 2f, 0.72f), Gold) }
        Box(Modifier.width(70.dp).height(1.dp).background(Brush.horizontalGradient(listOf(Gold, Color.Transparent))))
    }
}
