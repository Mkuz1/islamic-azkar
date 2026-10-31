package com.koussa.islamicazkar

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.view.View
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.koussa.islamicazkar.data.AzkarRepository
import com.koussa.islamicazkar.data.Cities
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Reading
import com.koussa.islamicazkar.data.Settings
import com.koussa.islamicazkar.data.ThemeMode
import com.koussa.islamicazkar.ui.AppState
import com.koussa.islamicazkar.ui.AzkarApp
import com.koussa.islamicazkar.ui.Celebration
import com.koussa.islamicazkar.ui.LocalHinge
import com.koussa.islamicazkar.ui.glass.ArabicUiFont
import com.koussa.islamicazkar.ui.glass.AzkarTheme
import com.koussa.islamicazkar.ui.glass.CalligraphyFont
import com.koussa.islamicazkar.ui.glass.LatinUiFont
import com.koussa.islamicazkar.ui.glass.starPath
import com.koussa.islamicazkar.widget.AzkarCardsWidget
import com.koussa.islamicazkar.widget.CounterStatsWidget
import com.koussa.islamicazkar.widget.DigitalClockWidget
import com.koussa.islamicazkar.widget.HijriDateWidget
import com.koussa.islamicazkar.widget.IslamicClockWidget
import com.koussa.islamicazkar.widget.PrayerTimesWidget
import com.koussa.islamicazkar.widget.ProgressWidget
import com.koussa.islamicazkar.widget.ScriptureWidget
import com.koussa.islamicazkar.widget.TasbihWidget
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Google Play store graphics, drawn from the real app and widgets: eight captioned phone
 * screenshots per language (1080×1920) and a feature graphic per language (1024×500).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StoreGraphicsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val out = File("build/store").apply { mkdirs() }

    private val night = Color(0xFF0A1C15)
    private val deep = Color(0xFF1B3A2F)
    private val gold = Color(0xFFD6B36A)
    private val goldLight = Color(0xFFF1DBA3)

    private class Caption(val ar: Pair<String, String>, val en: Pair<String, String>, val fr: Pair<String, String>) {
        fun of(l: Lang) = when (l) { Lang.AR -> ar; Lang.EN -> en; Lang.FR -> fr }
    }

    private val captions = listOf(
        Caption("حصن المسلم بحُلّةٍ زجاجية" to "أذكار اليوم والليلة بتصميمٍ أنيقٍ هادئ",
            "Hisn al-Muslim, beautifully crafted" to "Daily azkar in a calm, elegant design",
            "La Citadelle du musulman, sublimée" to "Les invocations du quotidien, dans un design apaisant"),
        Caption("أذكار الصباح والمساء" to "مع العدّاد والترجمة والنطق",
            "Morning & evening azkar" to "With a counter, transliteration and meaning",
            "Adhkar du matin et du soir" to "Avec compteur, translittération et traduction"),
        Caption("مواقيت الصلاة على شاشتك" to "عدٌّ تنازليّ للصلاة القادمة مع التاريخ الهجري",
            "Prayer times on your home screen" to "A countdown to the next prayer, plus the Hijri date",
            "Les horaires de prière sur votre écran" to "Compte à rebours et date hégirienne"),
        Caption("آيةٌ وحديثٌ وشعرٌ وحكمة" to "اضغط لتتجدّد الكلمات، بخطٍّ عربيٍّ أصيل",
            "Quran, hadith, poetry & wisdom" to "Tap for a new one, in true Arabic calligraphy",
            "Coran, hadith, poésie & sagesse" to "Touchez pour en découvrir une autre"),
        Caption("خطوطٌ عربية وألوانٌ هادئة" to "رقعة ونسخ وكوفي ونستعليق، بسبعة ألوان",
            "Arabic calligraphy, your colours" to "Ruqʿah, Naskh, Kufic & Nastaʿlīq in seven calm palettes",
            "Calligraphie arabe, vos couleurs" to "Roqʿa, Naskh, Coufique & Nastaʿlîq, sept palettes"),
        Caption("مسبحةٌ تعدّ معك" to "حتى من شاشتك الرئيسية، مع إحصاء الأسبوع",
            "A tasbih that counts with you" to "Even from your home screen, with weekly stats",
            "Un chapelet qui compte avec vous" to "Même depuis l’écran d’accueil, avec vos statistiques"),
        Caption("شارك الذكر صورةً جميلة" to "بطاقاتٌ أنيقة جاهزة للمشاركة",
            "Share any dhikr as an image" to "Elegant cards, ready to share",
            "Partagez une invocation en image" to "De belles cartes prêtes à partager"),
        Caption("فرحةُ الإتمام" to "تهنئةٌ ودعاءٌ بعد كلّ وِرد، لتبقى على الذكر",
            "Celebrate every completed reading" to "A prayer and encouragement to keep you going",
            "Célébrez chaque lecture achevée" to "Une invocation et un encouragement pour persévérer"),
    )

    /** A RemoteViews widget drawn to a bitmap at its dp size (attached so live views fill in). */
    private val window by lazy { org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup().get() }

    private fun widget(@Suppress("UNUSED_PARAMETER") activity: ComponentActivity, v: RemoteViews, wDp: Int, hDp: Int): Bitmap {
        val dm = ctx.resources.displayMetrics
        val host = android.widget.FrameLayout(window)
        val view = v.apply(window, host)
        host.addView(view)
        window.setContentView(host)
        val w = (wDp * dm.density).toInt(); val h = (hDp * dm.density).toInt()
        view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, w, h)
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
    }

    @Composable
    private fun Backdrop(content: @Composable () -> Unit) {
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(Brush.verticalGradient(listOf(deep, night, Color(0xFF07140F))))
                drawCircle(Brush.radialGradient(listOf(gold.copy(alpha = 0.22f), Color.Transparent), center = Offset(size.width * 0.5f, size.height * 0.12f), radius = size.width * 0.8f), radius = size.width * 0.8f, center = Offset(size.width * 0.5f, size.height * 0.12f))
                // A faint lattice of eight-pointed stars.
                val step = size.width / 6
                var y = step / 2
                while (y < size.height) {
                    var x = step / 2
                    while (x < size.width) {
                        drawPath(starPath(Offset(x, y), step * 0.32f, 0.72f), Color.White.copy(alpha = 0.035f), style = Stroke(1.2f))
                        x += step
                    }
                    y += step
                }
            },
        ) { content() }
    }

    @Composable
    private fun Headline(lang: Lang, c: Pair<String, String>, modifier: Modifier = Modifier) {
        val ar = lang == Lang.AR
        Column(modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                c.first, color = Color.White, textAlign = TextAlign.Center,
                fontFamily = if (ar) ArabicUiFont else LatinUiFont, fontWeight = FontWeight.Bold,
                fontSize = if (ar) 30.sp else 27.sp, lineHeight = if (ar) 42.sp else 33.sp,
            )
            Text(
                c.second, color = goldLight, textAlign = TextAlign.Center,
                fontFamily = if (ar) ArabicUiFont else LatinUiFont,
                fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    /** A phone frame showing [content] laid out at full phone size and scaled down. */
    @Composable
    private fun Phone(width: Dp, rtl: Boolean, content: @Composable () -> Unit) {
        val inner = width - 14.dp
        val k = inner.value / 400f
        Box(
            Modifier.width(width).height(width * 2.15f)
                .shadow(30.dp, RoundedCornerShape(42.dp))
                .clip(RoundedCornerShape(42.dp))
                .background(Color(0xFF0E0E10))
                .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.08f))), RoundedCornerShape(42.dp))
                .padding(7.dp)
                .clip(RoundedCornerShape(35.dp)),
        ) {
            Box(
                Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(400.dp, 860.dp)
                    .graphicsLayer { scaleX = k; scaleY = k; transformOrigin = TransformOrigin(0f, 0f) },
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) { content() }
            }
        }
    }

    /** A home screen: a soft photographic wallpaper with widgets on it. */
    @Composable
    private fun HomeScreen(widgets: List<List<Bitmap>>) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xFF5B7FA6), Color(0xFFB98A6E), Color(0xFF3E6B5A)), start = Offset(0f, 0f), end = Offset(1200f, 2600f)),
            ),
        ) {
            Column(Modifier.fillMaxSize().padding(top = 56.dp, start = 18.dp, end = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                widgets.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { b ->
                            val w = (b.width / ctx.resources.displayMetrics.density).dp
                            val h = (b.height / ctx.resources.displayMetrics.density).dp
                            Image(b.asImageBitmap(), null, Modifier.size(w, h))
                        }
                    }
                }
            }
        }
    }

    @Config(sdk = [34], qualifiers = "w360dp-h640dp-xxhdpi")
    @Test fun screenshots() {
        val st = Settings.get(ctx)
        st.updateTheme(ThemeMode.LIGHT)
        st.updatePlace(Cities.all.first { it.name == "Beirut" })
        st.updatePalette(0); st.updateScript(0)
        val sp = Settings.prefs(ctx)
        val now = System.currentTimeMillis()
        val today = (now + java.util.TimeZone.getDefault().getOffset(now)) / 86_400_000L
        listOf(120, 66, 33, 233, 99, 165, 27).forEachIndexed { i, n -> sp.edit().putInt("tasbih_day_${today - 6 + i}", n).commit() }
        st.setTasbih(count = 27, target = 33, total = 1250)

        var slide by mutableStateOf(0)
        var lang by mutableStateOf(Lang.AR)
        var widgetRows by mutableStateOf<List<List<Bitmap>>>(emptyList())
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AzkarTheme(st.theme, st.lang) {
                CompositionLocalProvider(LocalHinge provides null, LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Backdrop {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(Modifier.height(40.dp))
                            Headline(lang, captions[slide].of(lang))
                            Spacer(Modifier.height(26.dp))
                            run {
                                Phone(282.dp, rtl = lang == Lang.AR) {
                                    key(slide, lang) {
                                        val app = remember {
                                            AppState(st).also { a ->
                                                when (slide) {
                                                    1 -> a.open(Reading.Morning)
                                                    5 -> a.selectTab(2)
                                                    6 -> { a.open(Reading.Morning); a.shareTarget = AzkarRepository.get(ctx).morning[7] to com.koussa.islamicazkar.i18n.strings(lang).morningAzkar }
                                                    7 -> { a.open(Reading.Morning); a.celebration = Celebration(Reading.Morning, com.koussa.islamicazkar.i18n.strings(lang).morningAzkar, 23, 455, 7, 1) }
                                                }
                                            }
                                        }
                                        if (slide in 2..4) HomeScreen(widgetRows) else AzkarApp(app)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        val activity = rule.activity
        for (l in listOf(Lang.AR, Lang.EN, Lang.FR)) {
            rule.runOnUiThread { st.updateLang(l) }
            for (i in captions.indices) {
                widgetRows = when (i) {
                    2 -> listOf(
                        listOf(widget(activity, PrayerTimesWidget.views(ctx, 301, 364 to 180), 364, 180)),
                        listOf(widget(activity, HijriDateWidget.views(ctx, 302, 175 to 175), 175, 175), widget(activity, IslamicClockWidget.views(ctx, 303), 175, 175)),
                        listOf(widget(activity, PrayerTimesWidget.views(ctx, 304, 364 to 80), 364, 80)),
                        listOf(widget(activity, DigitalClockWidget.views(ctx, 305, 364 to 80), 364, 80)),
                    )
                    3 -> {
                        sp.edit().putInt("quote_src_311", 4).putInt("quote_src_312", 1).putInt("quote_src_313", 5).putInt("cards_cat_314", 0).commit()
                        listOf(
                            listOf(widget(activity, ScriptureWidget.views(ctx, 311, 364 to 190, now = 3 * 3_600_000L), 364, 190)),
                            listOf(widget(activity, ScriptureWidget.views(ctx, 313, 175 to 175, now = 6 * 3_600_000L), 175, 175), widget(activity, ProgressWidget.views(ctx, 315, 175 to 175), 175, 175)),
                            listOf(widget(activity, ScriptureWidget.views(ctx, 312, 364 to 190, now = 5 * 3_600_000L), 364, 190)),
                            listOf(widget(activity, ScriptureWidget.views(ctx, 316, 364 to 80, now = 2 * 3_600_000L), 364, 80)),
                        )
                    }
                    4 -> {
                        val rows = mutableListOf<List<Bitmap>>()
                        for ((script, palette) in listOf(0 to 0, 2 to 1, 3 to 3, 1 to 4)) {
                            st.updateScript(script); st.updatePalette(palette)
                            rows += listOf(widget(activity, PrayerTimesWidget.views(ctx, 320 + script, 364 to 150), 364, 150))
                        }
                        st.updateScript(0); st.updatePalette(0)
                        rows
                    }
                    else -> emptyList()
                }
                rule.runOnUiThread { slide = i; lang = l }
                rule.mainClock.advanceTimeByFrame()
                rule.waitForIdle()
                rule.mainClock.advanceTimeBy(1500)
                Thread.sleep(if (i == 6) 2500 else 200)  // the share preview renders in the background
                rule.mainClock.advanceTimeBy(1000)
                rule.waitForIdle()
                rule.onRoot().captureRoboImage(File(out, "phone_${l.code}_${i + 1}.png").path)
            }
        }
        st.updatePlace(null)
    }

    @Config(sdk = [34], qualifiers = "w1024dp-h500dp-mdpi")
    @Test fun featureGraphic() {
        val st = Settings.get(ctx)
        st.updatePlace(Cities.all.first { it.name == "Beirut" })
        val iconFile = File("../../store/icon_512.png")
        val icon = BitmapFactory.decodeFile(iconFile.path)
        var lang by mutableStateOf(Lang.AR)
        var widgets by mutableStateOf<List<Bitmap>>(emptyList())
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AzkarTheme(st.theme, st.lang) {
                CompositionLocalProvider(LocalHinge provides null, LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Backdrop {
                        Row(Modifier.fillMaxSize().padding(horizontal = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.width(470.dp)) {
                                if (icon != null) Image(icon.asImageBitmap(), null, Modifier.size(104.dp).clip(RoundedCornerShape(26.dp)))
                                Text("أذكار إسلامية", fontFamily = CalligraphyFont, fontWeight = FontWeight.Bold, color = goldLight, fontSize = 58.sp, modifier = Modifier.padding(top = 14.dp))
                                Text("Islamic Azkar", fontFamily = LatinUiFont, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 30.sp)
                                Text(
                                    when (lang) { Lang.AR -> "حصن المسلم · مواقيت الصلاة · أدوات بخط عربي"; Lang.EN -> "Hisn al-Muslim · Prayer times · Calligraphy widgets"; Lang.FR -> "Citadelle du musulman · Prière · Calligraphie" },
                                    fontFamily = if (lang == Lang.AR) ArabicUiFont else LatinUiFont, color = goldLight.copy(alpha = 0.9f), fontSize = 19.sp, modifier = Modifier.padding(top = 10.dp),
                                )
                            }
                            Box(Modifier.fillMaxSize()) {
                                widgets.getOrNull(0)?.let { Image(it.asImageBitmap(), null, Modifier.offset(30.dp, 40.dp).size(380.dp, 188.dp).rotate(-4f)) }
                                widgets.getOrNull(1)?.let { Image(it.asImageBitmap(), null, Modifier.offset(120.dp, 250.dp).size(330.dp, 172.dp).rotate(3f)) }
                            }
                        }
                    }
                }
            }
        }
        val activity = rule.activity
        for (l in listOf(Lang.AR, Lang.EN, Lang.FR)) {
            rule.runOnUiThread { st.updateLang(l) }
            Settings.prefs(ctx).edit().putInt("quote_src_401", 4).commit()
            val w = listOf(
                widget(activity, PrayerTimesWidget.views(ctx, 400, 364 to 180, now = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Beirut")).apply { set(2026, 9, 31, 14, 5, 0) }.timeInMillis), 364, 180),
                widget(activity, ScriptureWidget.views(ctx, 401, 330 to 172, now = 3 * 3_600_000L), 330, 172),
            )
            rule.runOnUiThread { lang = l; widgets = w }
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            rule.mainClock.advanceTimeBy(1500)
            rule.onRoot().captureRoboImage(File(out, "feature_${l.code}.png").path)
        }
        st.updatePlace(null)
    }
}
