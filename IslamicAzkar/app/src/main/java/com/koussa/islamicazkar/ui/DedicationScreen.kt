package com.koussa.islamicazkar.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.koussa.islamicazkar.data.AzkarRepository
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Reading
import com.koussa.islamicazkar.data.Settings
import com.koussa.islamicazkar.ui.glass.ArabicFont
import com.koussa.islamicazkar.ui.glass.tvClickable
import com.koussa.islamicazkar.ui.glass.ArabicUiFont
import com.koussa.islamicazkar.ui.glass.CalligraphyFont
import com.koussa.islamicazkar.ui.glass.Glass
import com.koussa.islamicazkar.ui.glass.QuranFont
import com.koussa.islamicazkar.ui.glass.pressable
import com.koussa.islamicazkar.ui.glass.LocalBackdrop
import com.koussa.islamicazkar.ui.glass.backdropSource
import com.koussa.islamicazkar.ui.glass.glass
import com.koussa.islamicazkar.ui.glass.rememberBackdrop
import com.koussa.islamicazkar.ui.glass.starPath

// The Mushaf app's opening page: a dark green illuminated card over a geometric pattern.
private val Night = Color(0xFF0A1C15)
private val CardTop = Color(0xFF26473C)
private val CardBottom = Color(0xFF1B3A2F)
private val Gold = Color(0xFFD6B36A)
private val GoldLight = Color(0xFFF1DBA3)
private val GoldDeep = Color(0xFFBF9447)
private val Ink = Color(0xFF15281F)
private val GoldBrush = Brush.verticalGradient(listOf(GoldLight, GoldDeep))

/** The dedication, shown in Arabic on the opening page whatever the language. */
const val DEDICATION_AR = "صدقة جارية عن محمد الكوسا وديمة غنوم وعائلاتهم"
private const val DEDICATION_NAMES_AR = "عن محمد الكوسا وديمة غنوم وعائلاتهم"

private data class Dedication(
    val transliteration: String?,
    /** Translation of the Arabic dedication line (none in Arabic). */
    val names: String?,
    val duaMeaning: String?,
    val hadithMeaning: String?,
    val begin: String,
)

private fun dedication(lang: Lang) = when (lang) {
    Lang.EN -> Dedication(
        "SADAQA JARIYAH", "On behalf of Mohamad Al-Koussa, Dima Ghannoum and their families",
        "O Allah, place it in the scale of their good deeds.",
        "“When a person dies, their deeds end except for three: an ongoing charity…” — Muslim",
        "Begin",
    )
    Lang.FR -> Dedication(
        "SADAQA JARIYA", "Au nom de Mohamad Al-Koussa, Dima Ghannoum et leurs familles",
        "Ô Allah, place-la dans la balance de leurs bonnes actions.",
        "« Quand l'homme meurt, ses œuvres cessent sauf trois : une aumône continue… » — Muslim",
        "Commencer",
    )
    Lang.AR -> Dedication(null, null, null, null, "ابدأ")
}

@Composable
fun DedicationScreen(onContinue: () -> Unit, onOpen: (Reading) -> Unit = {}) {
    val ctx = LocalContext.current
    val settings = remember { Settings.get(ctx) }
    val lang = Glass.lang
    val text = dedication(lang)
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }

    val recommended = remember { recommendedReading() }
    val s = Glass.s
    val data = remember { AzkarRepository.get(ctx) }
    val recTitle = when (recommended) {
        Reading.Morning -> s.morningAzkar
        Reading.Evening -> s.eveningAzkar
        is Reading.OfChapter -> data.chapterById[recommended.id]?.title(lang) ?: s.book
    }
    val recCount = when (recommended) {
        Reading.Morning -> data.morning.size
        Reading.Evening -> data.evening.size
        is Reading.OfChapter -> data.chapterById[recommended.id]?.items?.size ?: 0
    }

    val backdrop = rememberBackdrop()
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().backdropSource(backdrop).background(Night)) {
            // A faint gold light behind the card so the glass has something to bend.
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height * 0.32f)
                val r = size.minDimension * 0.75f
                drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = 0.10f), Color.Transparent), c, r), r, c)
            }
            GeometricPattern()
        }
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
        BoxWithConstraints(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            // The card follows the window: full width on phones, a centred column on
            // unfolded foldables and tablets (narrower in Arabic, as in the Mushaf).
            // Phones: the card follows the width. Tablets and unfolded foldables: it grows with the
            // screen, then `fit` shrinks it if it would overflow.
            val large = maxWidth >= 600.dp && maxHeight >= 600.dp
            val grow = if (large) minOf(maxWidth / 640.dp, maxHeight / 760.dp, 1.7f).coerceAtLeast(1f) else 1f
            val cardWidth = min(maxWidth, (if (lang == Lang.AR) 520.dp else 760.dp) * grow)
            val kw = if (large) grow * 1.05f else (cardWidth / 560.dp).coerceIn(0.78f, 1.12f)
            // If the card is taller than the screen (short phones, landscape), shrink everything until
            // it fits instead of scrolling.
            val scroll = rememberScrollState()
            var fit by remember(maxWidth, maxHeight, lang) { mutableFloatStateOf(1f) }
            var viewH by remember { mutableIntStateOf(0) }
            LaunchedEffect(scroll.maxValue, viewH) {
                if (scroll.maxValue > 2 && viewH > 300 && kw * fit > 0.5f) {
                    fit = maxOf(fit * viewH / (viewH + scroll.maxValue) * 0.98f, 0.5f / kw)
                }
            }
            val k = (kw * fit).coerceAtLeast(0.5f)
            Column(
                Modifier
                    .width(cardWidth)
                    .heightIn(max = maxHeight)
                    .onSizeChanged { viewH = it.height }
                    .alpha(appear.value)
                    .glass(RoundedCornerShape(30.dp), blurRadius = 14.dp, elevation = 24.dp, tint = CardTop.copy(alpha = 0.80f), fallback = CardTop)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Brush.verticalGradient(listOf(Color.Transparent, CardBottom.copy(alpha = 0.45f))))
                    .border(1.dp, Gold.copy(alpha = 0.55f), RoundedCornerShape(30.dp))
                    .padding(6.dp)
                    .border(0.6.dp, Gold.copy(alpha = 0.22f), RoundedCornerShape(25.dp))
                    .verticalScroll(scroll)
                    .padding(horizontal = (24 * minOf(k, 1f)).dp, vertical = (30 * k).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", fontFamily = QuranFont, fontSize = (24 * k).sp,
                        color = Gold, textAlign = TextAlign.Center,
                    )
                }
                Ornament(Modifier.padding(vertical = (14 * k).dp))

                Text(
                    "صدقة جارية", fontFamily = CalligraphyFont, fontWeight = FontWeight.Bold,
                    fontSize = (66 * k).sp, lineHeight = (96 * k).sp, textAlign = TextAlign.Center,
                    style = TextStyle(brush = GoldBrush, shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 6f), 18f)),
                    modifier = Modifier.padding(top = (10 * k).dp),
                )
                text.transliteration?.let {
                    Text(
                        it, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = (13 * k).sp,
                        letterSpacing = (6 * k).sp, color = Gold.copy(alpha = 0.85f), modifier = Modifier.padding(top = (4 * k).dp),
                    )
                }
                // The Arabic dedication appears in every language…
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        DEDICATION_NAMES_AR, fontFamily = ArabicUiFont, fontWeight = FontWeight.Bold,
                        fontSize = (27 * k).sp, lineHeight = (40 * k).sp, color = Color.White, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = (18 * k).dp),
                    )
                }
                // …with its translation underneath in English and French.
                text.names?.let {
                    Text(
                        it, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold,
                        fontSize = (19 * k).sp, lineHeight = (26 * k).sp, color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Ornament(Modifier.padding(vertical = (16 * k).dp))

                Text(
                    "اللَّهُمَّ اجْعَلْهُ فِي مِيزَانِ حَسَنَاتِهِمْ", fontFamily = ArabicFont, fontWeight = FontWeight.Bold,
                    fontSize = (22 * k).sp, color = Gold, textAlign = TextAlign.Center,
                )
                text.duaMeaning?.let {
                    Text(
                        it, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold,
                        fontSize = (15 * k).sp, color = Color.White.copy(alpha = 0.82f), textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    "إِذَا مَاتَ الإِنْسَانُ انْقَطَعَ عَمَلُهُ إِلَّا مِنْ ثَلَاثٍ: صَدَقَةٍ جَارِيَةٍ… — رواه مسلم",
                    fontFamily = ArabicFont, fontSize = (15 * k).sp, lineHeight = (26 * k).sp,
                    color = Color.White.copy(alpha = 0.62f), textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = (16 * k).dp),
                )
                text.hadithMeaning?.let {
                    Text(
                        it, fontFamily = FontFamily.Serif, fontSize = (12 * k).sp, lineHeight = (17 * k).sp,
                        color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height((24 * k).dp))
                LanguagePicker(lang) { settings.updateLang(it) }
                Spacer(Modifier.height((18 * k).dp))

                Text(
                    text.begin, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .pressable(onContinue, squish = true)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.verticalGradient(listOf(GoldLight, Gold)))
                        .padding(horizontal = 40.dp, vertical = (13 * minOf(k, 1f)).dp),
                )
                Text(
                    "$recTitle · ${s.azkar(recCount)}", color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp,
                    modifier = Modifier.padding(top = (12 * k).dp).pressable({ onOpen(recommended) }),
                )
            }
        }
        }
    }
}

private fun recommendedReading(): Reading = when (currentMoment()) {
    Moment.MORNING -> Reading.Morning
    Moment.NOON -> Reading.OfChapter(25)
    Moment.EVENING -> Reading.Evening
    Moment.NIGHT -> Reading.OfChapter(28)
}

/** Thin gold rule with a small rosette in the middle. */
@Composable
private fun Ornament(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Box(Modifier.width(110.dp).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Gold.copy(alpha = 0.8f)))))
        Canvas(Modifier.padding(horizontal = 10.dp).size(10.dp)) {
            drawPath(starPath(center, size.minDimension / 2f, 0.72f), Gold)
        }
        Box(Modifier.width(110.dp).height(1.dp).background(Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.8f), Color.Transparent))))
    }
}

/** English | Français | العربية, as on the Mushaf's opening page. */
@Composable
private fun LanguagePicker(current: Lang, onSelect: (Lang) -> Unit) {
    val options = listOf(Lang.EN to "English", Lang.FR to "Français", Lang.AR to "العربية")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier.clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = 0.09f)).padding(2.dp),
        ) {
            options.forEach { (l, label) ->
                val sel = l == current
                Text(
                    label, fontSize = 13.sp, color = Color.White.copy(alpha = if (sel) 1f else 0.7f),
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (sel) Color.White.copy(alpha = 0.16f) else Color.Transparent)
                        .then(Modifier.tvClickable { onSelect(l) })
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                )
            }
        }
    }
}

/** Interlaced eight-pointed stars, very faint, like the Mushaf's backdrop. */
@Composable
private fun GeometricPattern() {
    Canvas(Modifier.fillMaxSize()) {
        val step = 72.dp.toPx()
        val stroke = Stroke(1.dp.toPx())
        val c = Color.White.copy(alpha = 0.035f)
        var y = 0f
        while (y < size.height + step) {
            var x = 0f
            while (x < size.width + step) {
                drawPath(starPath(Offset(x, y), step * 0.5f, 0.78f), c, style = stroke)
                drawRect(c, topLeft = Offset(x - step * 0.18f, y - step * 0.18f), size = androidx.compose.ui.geometry.Size(step * 0.36f, step * 0.36f), style = stroke)
                x += step
            }
            y += step
        }
    }
}

