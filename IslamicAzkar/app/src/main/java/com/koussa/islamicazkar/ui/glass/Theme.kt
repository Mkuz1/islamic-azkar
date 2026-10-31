package com.koussa.islamicazkar.ui.glass

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.koussa.islamicazkar.R
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.ThemeMode
import com.koussa.islamicazkar.i18n.Strings
import com.koussa.islamicazkar.i18n.strings

/**
 * Apple's system palette (grouped backgrounds, label hierarchy, separators), tinted with
 * the Mushaf's green and gold. Content sits on plain surfaces; only the navigation layer
 * (tab bar, back button, sheets) is glass.
 */
@Immutable
data class GlassColors(
    val dark: Boolean,
    /** systemGroupedBackground */
    val background: Color,
    /** secondarySystemGroupedBackground — cards and list groups */
    val card: Color,
    /** tertiarySystemFill — search field, segmented track, idle chips */
    val fillGray: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val separator: Color,
    val accent: Color,
    val accentSoft: Color,
    val gold: Color,
    val onAccent: Color,
    // Navigation-layer glass
    val tint: Color,
    val fillStrong: Color,
    val rimLight: Color,
    val rimDark: Color,
    val highlight: Color,
    val shadow: Color,
    // Content glass panes (cards, lists) and the backdrop they refract
    val pane: Color,
    val paneFallback: Color,
    val wallTop: Color,
    val wallBottom: Color,
    val glowGreen: Color,
    val glowGold: Color,
    val pattern: Color,
)

val LightGlass = GlassColors(
    dark = false,
    background = Color(0xFFF2F2F7),
    card = Color.White,
    fillGray = Color(0x1F767680),
    text = Color.Black,
    text2 = Color(0x993C3C43),
    text3 = Color(0x4D3C3C43),
    separator = Color(0x4A3C3C43),
    accent = Color(0xFF1E6B52),
    accentSoft = Color(0x1A1E6B52),
    gold = Color(0xFFB08A3E),
    onAccent = Color.White,
    tint = Color(0xB8F9F9F9),
    fillStrong = Color(0xCCFFFFFF),
    rimLight = Color(0xCCFFFFFF),
    rimDark = Color(0x14000000),
    highlight = Color(0x66FFFFFF),
    shadow = Color(0x33000000),
    pane = Color(0x9EFFFFFF),
    paneFallback = Color(0xD9FFFFFF),
    wallTop = Color(0xFFE9F0EC),
    wallBottom = Color(0xFFF5F1E8),
    glowGreen = Color(0x4D1E6B52),
    glowGold = Color(0x40D6B36A),
    pattern = Color(0x0F1E6B52),
)

val DarkGlass = GlassColors(
    dark = true,
    background = Color.Black,
    card = Color(0xFF1C1C1E),
    fillGray = Color(0x3D767680),
    text = Color.White,
    text2 = Color(0x99EBEBF5),
    text3 = Color(0x4DEBEBF5),
    separator = Color(0xA6545458),
    accent = Color(0xFF5DC79C),
    accentSoft = Color(0x295DC79C),
    gold = Color(0xFFD9B66C),
    onAccent = Color(0xFF062118),
    tint = Color(0xB31C1C1E),
    fillStrong = Color(0xCC2C2C2E),
    rimLight = Color(0x33FFFFFF),
    rimDark = Color(0x0DFFFFFF),
    highlight = Color(0x14FFFFFF),
    shadow = Color(0x80000000),
    pane = Color(0x6B1C1C1E),
    paneFallback = Color(0xCC1C1C1E),
    wallTop = Color(0xFF07140F),
    wallBottom = Color(0xFF020504),
    glowGreen = Color(0x99204A3C),
    glowGold = Color(0x26D6B36A),
    pattern = Color(0x0AFFFFFF),
)

/** Inter: the open typeface closest to Apple's SF Pro, for Latin UI text. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun inter(weight: FontWeight) = Font(
    R.font.inter, weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val LatinUiFont = FontFamily(
    inter(FontWeight.Normal), inter(FontWeight.Medium), inter(FontWeight.SemiBold), inter(FontWeight.Bold),
)

/** A clean Arabic sans, close to SF Arabic. */
val ArabicUiFont = FontFamily(
    Font(R.font.ibmplexsansarabic_medium, FontWeight.Normal),
    Font(R.font.ibmplexsansarabic_medium, FontWeight.Medium),
    Font(R.font.ibmplexsansarabic_semibold, FontWeight.SemiBold),
    Font(R.font.ibmplexsansarabic_semibold, FontWeight.Bold),
)

/** Amiri: classical Naskh with full diacritics, for every dhikr. */
val ArabicFont = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold),
)

/** Quranic script for the basmala. */
val QuranFont = FontFamily(Font(R.font.amiri_quran))

/** Ruqaa calligraphy for «صدقة جارية» and the app name. */
val CalligraphyFont = FontFamily(Font(R.font.aref_ruqaa, FontWeight.Normal), Font(R.font.aref_ruqaa_bold, FontWeight.Bold))

/** Kept for call sites that predate per-language fonts. */
val UiFont: FontFamily get() = LatinUiFont

val LocalGlass = staticCompositionLocalOf { LightGlass }
val LocalStrings = staticCompositionLocalOf { strings(Lang.AR) }
val LocalLang = staticCompositionLocalOf { Lang.AR }

object Glass {
    val colors: GlassColors @Composable get() = LocalGlass.current
    val s: Strings @Composable get() = LocalStrings.current
    val lang: Lang @Composable get() = LocalLang.current
}

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun AzkarTheme(mode: ThemeMode, lang: Lang, content: @Composable () -> Unit) {
    val dark = isDark(mode)
    val g = if (dark) DarkGlass else LightGlass
    val scheme = if (dark) darkColorScheme(
        primary = g.accent, onPrimary = g.onAccent, secondary = g.gold,
        background = g.background, surface = g.card, onSurface = g.text, onBackground = g.text,
        surfaceContainerHigh = g.card, surfaceContainerHighest = Color(0xFF2C2C2E), outline = g.text3,
    ) else lightColorScheme(
        primary = g.accent, onPrimary = g.onAccent, secondary = g.gold,
        background = g.background, surface = g.card, onSurface = g.text, onBackground = g.text,
        surfaceContainerHigh = g.card, surfaceContainerHighest = Color(0xFFE5E5EA), outline = g.text3,
    )
    // SF-style type ramp: Large Title 34, Title2 22, Headline 17 semibold, Body 17, Footnote 13.
    val family = if (lang == Lang.AR) ArabicUiFont else LatinUiFont
    val base = TextStyle(fontFamily = family, color = g.text, letterSpacing = if (lang == Lang.AR) 0.sp else (-0.2).sp)
    val typography = Typography(
        displaySmall = base.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 41.sp, letterSpacing = if (lang == Lang.AR) 0.sp else 0.3.sp),
        headlineMedium = base.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp),
        titleLarge = base.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
        titleMedium = base.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
        bodyLarge = base.copy(fontSize = 17.sp, lineHeight = 22.sp),
        bodyMedium = base.copy(fontSize = 15.sp, lineHeight = 20.sp),
        bodySmall = base.copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = base.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = base.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
        labelSmall = base.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
    )
    CompositionLocalProvider(
        LocalGlass provides g,
        LocalStrings provides strings(lang),
        LocalLang provides lang,
        LocalLayoutDirection provides if (lang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography) {
            androidx.compose.material3.ProvideTextStyle(typography.bodyLarge, content)
        }
    }
}
