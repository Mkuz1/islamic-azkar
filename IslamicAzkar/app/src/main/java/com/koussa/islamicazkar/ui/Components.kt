package com.koussa.islamicazkar.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Diversity3
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Settings
import com.koussa.islamicazkar.data.Zikr
import com.koussa.islamicazkar.i18n.num
import com.koussa.islamicazkar.ui.glass.ArabicFont
import com.koussa.islamicazkar.ui.glass.Glass
import com.koussa.islamicazkar.ui.glass.ProgressRing
import com.koussa.islamicazkar.ui.glass.pane
import com.koussa.islamicazkar.ui.glass.pressable

/** Google TV / Android TV (Chromecast with Google TV, smart TVs): driven by the remote. */
@Composable
fun isTv(): Boolean =
    (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION

/** Content padding that clears the status bar (and the floating back button) and the floating tab bar.
 *  On TV the tab bar sits at the top and content keeps inside the TV-safe margins. */
@Composable
fun screenPadding(hasBack: Boolean = false, horizontal: Dp = 16.dp): PaddingValues {
    if (isTv()) return PaddingValues(start = horizontal + 32.dp, end = horizontal + 32.dp, top = 104.dp, bottom = 40.dp)
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(start = horizontal, end = horizontal, top = top + if (hasBack) 60.dp else 12.dp, bottom = bottom + 100.dp)
}

/** iOS large title (34pt bold) with an optional secondary line. */
@Composable
fun LargeTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    val g = Glass.colors
    Column(modifier.padding(horizontal = 4.dp).padding(top = 6.dp, bottom = 10.dp)) {
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.displaySmall, color = g.text)
        if (subtitle != null) Text(subtitle, color = g.text2, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

fun categoryIcon(key: String): ImageVector = when (key) {
    "prayer" -> Icons.Rounded.Mosque
    "sleep" -> Icons.Rounded.Bedtime
    "home" -> Icons.Rounded.Cottage
    "hardship" -> Icons.Rounded.Shield
    "illness" -> Icons.Rounded.HealthAndSafety
    "food" -> Icons.Rounded.Restaurant
    "family" -> Icons.Rounded.Diversity3
    "travel" -> Icons.Rounded.FlightTakeoff
    "nature" -> Icons.Rounded.Thunderstorm
    "hajj" -> Icons.Rounded.Explore
    "sunnah" -> Icons.AutoMirrored.Rounded.MenuBook
    "quran" -> Icons.Rounded.AutoStories
    else -> Icons.Rounded.AutoAwesome
}

/** Apple system colours, one per category, as in the iOS Settings app. */
fun categoryHue(key: String): Color = when (key) {
    "prayer" -> Color(0xFF34C759)
    "sleep" -> Color(0xFF5856D6)
    "home" -> Color(0xFFFF9500)
    "hardship" -> Color(0xFF007AFF)
    "illness" -> Color(0xFFFF3B30)
    "food" -> Color(0xFFFF9F0A)
    "family" -> Color(0xFFAF52DE)
    "travel" -> Color(0xFF30B0C7)
    "nature" -> Color(0xFF00C7BE)
    "hajj" -> Color(0xFFA2845E)
    "sunnah" -> Color(0xFF0A84FF)
    "quran" -> Color(0xFF1E6B52)
    else -> Color(0xFFFF2D55)
}

/** Settings-style icon square (kept under its old name for existing call sites). */
@Composable
fun IconChip(icon: ImageVector, hue: Color, size: Dp = 30.dp) = SymbolSquare(icon, hue, size)

fun View.tick(settings: Settings, strong: Boolean = false) {
    if (!settings.haptics) return
    performHapticFeedback(if (strong) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP)
}

fun Context.copyText(text: String) {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Islamic Azkar", text))
}

fun shareableText(z: Zikr, lang: Lang, title: String): String = buildString {
    append(z.ar)
    val m = z.meaning(lang)
    if (m.isNotBlank()) append("\n\n").append(m)
    append("\n\n— ").append(title).append(" · Islamic Azkar | أذكار إسلامية")
}

/**
 * One dhikr on a plain card: Arabic in Amiri, optional transliteration and translation,
 * and a tinted counter that counts down the prescribed repetitions. Tap anywhere to count.
 */
@Composable
fun ZikrCard(
    z: Zikr,
    index: Int?,
    total: Int?,
    remaining: Int,
    chapterTitle: String,
    onCount: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    onDone: () -> Unit = {},
) {
    val app = LocalApp.current
    val settings = app.settings
    val g = Glass.colors
    val s = Glass.s
    val lang = Glass.lang
    val ctx = LocalContext.current
    val view = LocalView.current
    val done = remaining == 0
    val dim by animateFloatAsState(if (done) 0.45f else 1f, tween(350), label = "dim")
    val fav = settings.isFavorite(z.id)
    // Azkar said 100 times or more get a single "Done" button instead of a tap counter.
    val bulk = z.repeat >= BULK_REPEAT

    Column(
        modifier
            .fillMaxWidth()
            .pressable(enabled = !done && !bulk, onClick = { view.tick(settings); onCount() })
            .pane(GroupShape)
            .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (index != null && total != null) Capsule("${lang.num(index)} ${s.ofTarget(lang.num(total))}")
            if (label != null) {
                Text(label, color = g.text2, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
            }
            Spacer(Modifier.weight(1f))
            SmallAction(Icons.Outlined.ContentCopy, s.copy) {
                ctx.copyText(shareableText(z, lang, chapterTitle)); app.toast = s.copied
            }
            SmallAction(Icons.Outlined.IosShare, s.share) { app.shareTarget = z to chapterTitle }
            SmallAction(if (fav) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder, null, tint = if (fav) Color(0xFFFF2D55) else g.accent) {
                view.tick(settings); settings.toggleFavorite(z.id)
            }
        }

        Column(Modifier.alpha(dim).padding(end = 8.dp)) {
            Text(
                z.ar,
                fontFamily = ArabicFont,
                fontSize = (23 * settings.textScale).sp,
                lineHeight = (23 * settings.textScale * 1.9f).sp,
                color = g.text,
                textAlign = TextAlign.Center,
                style = TextStyle(textDirection = TextDirection.Rtl),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
            )
            if (lang != Lang.AR && settings.showTranslit && z.translit.isNotBlank()) {
                Text(
                    z.translit, color = g.text2, fontStyle = FontStyle.Italic,
                    fontSize = (15 * settings.textScale).sp, lineHeight = (21 * settings.textScale).sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            val meaning = z.meaning(lang)
            if (settings.showMeaning && meaning.isNotBlank()) {
                Text(
                    meaning, color = g.text,
                    fontSize = (16 * settings.textScale).sp, lineHeight = (22 * settings.textScale).sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        z.ref?.let { ref ->
            Row(
                Modifier.fillMaxWidth().alpha(dim).padding(top = 12.dp, end = 8.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.MenuBook, null, tint = g.text3, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(ref[lang], color = g.text3, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.Center) {
            if (bulk && !done) DoneButton(z.repeat) { view.tick(settings, strong = true); onDone() }
            else Counter(remaining, z.repeat)
        }
    }
}

/** Repetitions from which a dhikr is marked complete with one tap rather than counted. */
const val BULK_REPEAT = 100

@Composable
private fun DoneButton(times: Int, onClick: () -> Unit) {
    val g = Glass.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(Glass.s.repeatTimes(times), color = g.text2, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(12.dp))
        Row(
            Modifier
                .pressable(onClick, squish = true)
                .clip(RoundedCornerShape(50))
                .background(g.accent)
                .padding(horizontal = 20.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Check, null, tint = g.onAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(Glass.s.completed, color = g.onAccent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SmallAction(icon: ImageVector, desc: String?, tint: Color = Glass.colors.accent, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).pressable(onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Counter(remaining: Int, total: Int) {
    val g = Glass.colors
    val lang = Glass.lang
    val done = remaining == 0
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(g.accentSoft)
            .padding(start = 5.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            ProgressRing((total - remaining) / total.toFloat(), Modifier.size(30.dp), stroke = 2.5.dp, track = g.accent.copy(alpha = 0.18f))
            AnimatedContent(
                targetState = remaining,
                transitionSpec = { (fadeIn(tween(150)) + scaleIn(initialScale = 0.7f)) togetherWith fadeOut(tween(100)) },
                label = "count",
            ) { r ->
                if (r == 0) Icon(Icons.Rounded.Check, null, tint = g.accent, modifier = Modifier.size(16.dp))
                else Text(lang.num(r), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = g.accent)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (done) Glass.s.completed else Glass.s.repeatTimes(total),
            color = g.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}
