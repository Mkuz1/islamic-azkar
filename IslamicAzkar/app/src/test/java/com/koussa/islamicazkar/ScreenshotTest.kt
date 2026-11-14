package com.koussa.islamicazkar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.koussa.islamicazkar.data.AzkarRepository
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Reading
import com.koussa.islamicazkar.data.Settings
import com.koussa.islamicazkar.data.ThemeMode
import com.koussa.islamicazkar.share.ShareCard
import com.koussa.islamicazkar.ui.AppState
import com.koussa.islamicazkar.ui.AzkarApp
import com.koussa.islamicazkar.ui.DedicationScreen
import com.koussa.islamicazkar.ui.Hinge
import com.koussa.islamicazkar.ui.LocalHinge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import com.koussa.islamicazkar.ui.glass.AzkarTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.input.key.Key
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders key screens, share images and widgets to PNGs under build/shots for visual review. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val out = File("build/shots").apply { mkdirs() }

    private fun shot(name: String, lang: Lang, theme: ThemeMode, content: @Composable (AppState) -> Unit, hinge: Hinge? = null, setup: (AppState) -> Unit = {}) {
        val st = Settings.get(ctx)
        st.updateLang(lang); st.updateTheme(theme)
        val app = AppState(st).also(setup)
        rule.mainClock.autoAdvance = false
        rule.setContent { AzkarTheme(st.theme, st.lang) { CompositionLocalProvider(LocalHinge provides hinge) { content(app) } } }
        rule.mainClock.advanceTimeBy(2500)
        rule.onRoot().captureRoboImage(File(out, "$name.png").path)
    }

    @Test fun dedication() = shot("01_dedication_ar", Lang.AR, ThemeMode.LIGHT, { DedicationScreen(onContinue = {}) })
    @Test fun dedicationEn() = shot("01_dedication_en", Lang.EN, ThemeMode.LIGHT, { DedicationScreen(onContinue = {}) })
    @Config(qualifiers = "w673dp-h841dp-xhdpi")
    @Test fun dedicationFold() = shot("01_dedication_fold_fr", Lang.FR, ThemeMode.LIGHT, { DedicationScreen(onContinue = {}) })
    @Test fun homeArLight() = shot("02_home_ar_light", Lang.AR, ThemeMode.LIGHT, { AzkarApp(it) })
    @Test fun homeEnDark() = shot("03_home_en_dark", Lang.EN, ThemeMode.DARK, { AzkarApp(it) })
    @Test fun homeFrLight() = shot("04_home_fr_light", Lang.FR, ThemeMode.LIGHT, { AzkarApp(it) })
    @Test fun readerMorningEn() = shot("05_reader_morning_en", Lang.EN, ThemeMode.LIGHT, { AzkarApp(it) }) { it.open(Reading.Morning) }
    @Test fun readerHundredAr() = shot("05b_reader_100_ar", Lang.AR, ThemeMode.LIGHT, { AzkarApp(it) }) { it.open(Reading.Morning, 10091) }
    @Test fun readerEveningArDark() = shot("06_reader_evening_ar_dark", Lang.AR, ThemeMode.DARK, { AzkarApp(it) }) { it.open(Reading.Evening) }
    @Test fun category() = shot("07_category_fr", Lang.FR, ThemeMode.DARK, { AzkarApp(it) }) { it.push(com.koussa.islamicazkar.ui.Route.Category("hardship")) }
    @Test fun tasbih() = shot("08_tasbih_ar_dark", Lang.AR, ThemeMode.DARK, { AzkarApp(it) }) { it.settings.setTasbih(count = 40, target = 33, total = 1250); it.selectTab(2) }
    @Test fun settings() = shot("09_settings_en", Lang.EN, ThemeMode.LIGHT, { AzkarApp(it) }) { it.selectTab(4) }
    @Config(qualifiers = "w411dp-h4600dp-xhdpi")
    @Test fun settingsFullEn() = shot("09b_settings_full_en", Lang.EN, ThemeMode.LIGHT, { AzkarApp(it) }) { it.settings.updatePlace(com.koussa.islamicazkar.data.Cities.all.first { c -> c.name == "Beirut" }); it.selectTab(4) }
    @Config(qualifiers = "w411dp-h4600dp-xhdpi")
    @Test fun settingsFullAr() = shot("09c_settings_full_ar_dark", Lang.AR, ThemeMode.DARK, { AzkarApp(it) }) { it.settings.updatePlace(com.koussa.islamicazkar.data.Cities.all.first { c -> c.name == "Beirut" }); it.selectTab(4) }
    @Config(qualifiers = "w411dp-h3000dp-xhdpi")
    @Test fun widgetsTabAr() = shot("09d_widgets_tab_ar", Lang.AR, ThemeMode.LIGHT, { AzkarApp(it) }) { it.selectTab(4); it.push(com.koussa.islamicazkar.ui.Route.WidgetsRoute) }
    @Test fun widgetsTabEnDark() = shot("09e_widgets_tab_en_dark", Lang.EN, ThemeMode.DARK, { AzkarApp(it) }) { it.selectTab(4); it.push(com.koussa.islamicazkar.ui.Route.WidgetsRoute) }
    @Test fun recordEn() = shot("09f_record_en", Lang.EN, ThemeMode.LIGHT, { AzkarApp(it) }) {
        listOf("morning", "c28", "c25").forEach { k -> com.koussa.islamicazkar.data.AzkarHistory.markReading(ctx, k) }
        it.selectTab(3)
    }
    @Test fun recordArDark() = shot("09g_record_ar_dark", Lang.AR, ThemeMode.DARK, { AzkarApp(it) }) {
        listOf("evening", "c28").forEach { k -> com.koussa.islamicazkar.data.AzkarHistory.markReading(ctx, k) }
        it.selectTab(3)
    }
    // Google TV / Android TV: 1080p TV (960 x 540 dp), driven by the remote's D-pad.
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    @Test fun tvHomeEn() = shot("30_tv_home_en", Lang.EN, ThemeMode.DARK, { AzkarApp(it) })
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    @Test fun tvHomeFocusAr() = tvShot("31_tv_home_focus_ar", Lang.AR, listOf(Key.DirectionDown, Key.DirectionDown, Key.DirectionDown)) { }
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    @Test fun tvTabsFocusEn() = tvShot("32_tv_tabs_focus_en", Lang.EN, listOf(Key.DirectionDown, Key.DirectionUp, Key.DirectionUp, Key.DirectionRight)) { }
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    @Test fun tvReaderEn() = tvShot("33_tv_reader_en", Lang.EN, listOf(Key.DirectionDown, Key.DirectionDown)) { it.open(Reading.Morning) }
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    @Test fun tvRecordAr() = tvShot("34_tv_record_ar", Lang.AR, listOf(Key.DirectionDown)) {
        listOf("morning", "c28").forEach { k -> com.koussa.islamicazkar.data.AzkarHistory.markReading(ctx, k) }
        it.selectTab(3)
    }
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    @Test fun tvDedication() = shot("35_tv_dedication_en", Lang.EN, ThemeMode.DARK, { DedicationScreen(onContinue = {}) })

    /** A TV screen after pressing these remote keys (the focused control shows its highlight). */
    @OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
    private fun tvShot(name: String, lang: Lang, keys: List<Key>, setup: (AppState) -> Unit) {
        val st = Settings.get(ctx)
        st.updateLang(lang); st.updateTheme(ThemeMode.DARK)
        val app = AppState(st).also(setup)
        rule.mainClock.autoAdvance = false
        rule.setContent { AzkarTheme(st.theme, st.lang) { AzkarApp(app) } }
        rule.mainClock.advanceTimeBy(1500)
        for (k in keys) {
            rule.onRoot().performKeyInput { pressKey(k) }
            rule.mainClock.advanceTimeBy(400)
        }
        rule.mainClock.advanceTimeBy(800)
        rule.onRoot().captureRoboImage(File(out, "$name.png").path)
    }

    @Test fun homePrayerAr() = shot("02b_home_prayer_ar", Lang.AR, ThemeMode.LIGHT, { AzkarApp(it) }) { it.settings.updatePlace(com.koussa.islamicazkar.data.Cities.all.first { c -> c.name == "Beirut" }) }
    @Test fun shareSheet() = shot("10_share_sheet_en", Lang.EN, ThemeMode.LIGHT, { AzkarApp(it) }) {
        it.open(Reading.Morning)
        val z = AzkarRepository.get(ctx).morning[7]
        it.shareTarget = z to "Morning Azkar"
    }

    // Galaxy Z Fold inner display, unfolded flat: split view.
    @Config(qualifiers = "w673dp-h841dp-xhdpi")
    @Test fun foldUnfolded() = shot("15_fold_unfolded_en", Lang.EN, ThemeMode.LIGHT, { AzkarApp(it) },
        hinge = Hinge(vertical = true, start = 336.dp, end = 337.dp, separating = false, halfOpened = false)) { it.push(com.koussa.islamicazkar.ui.Route.Category("prayer")) }

    // Book posture, half-opened: panes meet exactly at the hinge.
    @Config(qualifiers = "w673dp-h841dp-xhdpi")
    @Test fun foldBook() = shot("16_fold_book_ar_dark", Lang.AR, ThemeMode.DARK, { AzkarApp(it) },
        hinge = Hinge(vertical = true, start = 336.dp, end = 337.dp, separating = true, halfOpened = true)) { it.open(Reading.Morning) }

    // Tabletop posture: tasbih bead above the fold, controls below.
    @Config(qualifiers = "w673dp-h841dp-xhdpi")
    @Test fun foldTabletop() = shot("17_fold_tabletop_tasbih", Lang.FR, ThemeMode.DARK, { AzkarApp(it) },
        hinge = Hinge(vertical = false, start = 420.dp, end = 421.dp, separating = true, halfOpened = true)) { it.selectTab(2) }

    // Galaxy Z Fold cover screen: narrow phone.
    @Config(qualifiers = "w344dp-h882dp-xxhdpi")
    @Test fun foldCover() = shot("18_fold_cover_fr", Lang.FR, ThemeMode.LIGHT, { AzkarApp(it) })

    // Tablet landscape.
    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    @Test fun tablet() = shot("19_tablet_landscape_en_dark", Lang.EN, ThemeMode.DARK, { AzkarApp(it) }) { it.open(Reading.Evening) }

    @Test fun celebrationAr() = shot("20_celebration_ar", Lang.AR, ThemeMode.LIGHT, { AzkarApp(it) }) {
        it.open(Reading.Morning)
        it.celebration = com.koussa.islamicazkar.ui.Celebration(Reading.Morning, "أذكار الصباح", 23, 455, 3, 0)
    }
    @Test fun celebrationEnDark() = shot("21_celebration_en_dark", Lang.EN, ThemeMode.DARK, { AzkarApp(it) }) {
        it.open(Reading.Evening)
        it.celebration = com.koussa.islamicazkar.ui.Celebration(Reading.Evening, "Evening Azkar", 21, 362, 1, 2)
    }

    @Test fun shareImages() {
        val data = AzkarRepository.get(ctx)
        val cases = listOf(
            Triple(data.morning[8], Lang.AR, "أذكار الصباح"),
            Triple(data.morning[10], Lang.EN, "Morning Azkar"),
            Triple(data.chapterById.getValue(34).items[0], Lang.FR, "Contre l'angoisse et la tristesse"),
            Triple(data.chapterById.getValue(130).items[3], Lang.EN, "The virtue of remembering Allah"),
        )
        cases.forEachIndexed { i, (z, lang, title) ->
            val bmp = ShareCard.render(ctx, z, title, i, lang, withMeaning = true)
            File(out, "11_share_image_$i.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun azkarWidgets() {
        val dm = ctx.resources.displayMetrics
        fun draw(v: android.widget.RemoteViews, wDp: Int, hDp: Int, name: String, wallpaper: Boolean = true) {
            val view = v.apply(ctx, android.widget.FrameLayout(ctx))
            val w = (wDp * dm.density).toInt(); val h = (hDp * dm.density).toInt()
            view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, w, h)
            val pad = (12 * dm.density).toInt()
            val bmp = Bitmap.createBitmap(w + pad * 2, h + pad * 2, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            // A photo-like wallpaper so the glass translucency is visible.
            val p = android.graphics.Paint().apply {
                shader = android.graphics.LinearGradient(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat(),
                    intArrayOf(0xFF5B7FA6.toInt(), 0xFFB98A6E.toInt(), 0xFF3E6B5A.toInt()), null, android.graphics.Shader.TileMode.CLAMP)
            }
            if (wallpaper) c.drawRect(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat(), p)
            c.translate(pad.toFloat(), pad.toFloat())
            view.draw(c)
            File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        fun at(h: Int) = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, h); set(java.util.Calendar.MINUTE, 40) }
        val W = com.koussa.islamicazkar.widget.AzkarListWidget
        for (lang in listOf(Lang.AR, Lang.EN)) {
            Settings.get(ctx).updateLang(lang)
            val l = lang.code
            draw(W.build(ctx, com.koussa.islamicazkar.widget.WidgetSet.MORNING, 1, compact = false, now = at(6)), 330, 170, "30_widget_morning_$l")
            draw(W.build(ctx, com.koussa.islamicazkar.widget.WidgetSet.DAY, 2, compact = false, now = at(13)), 330, 170, "31_widget_day_$l")
            draw(W.build(ctx, com.koussa.islamicazkar.widget.WidgetSet.NIGHT, 3, compact = false, now = at(21)), 330, 170, "32_widget_night_$l")
            for ((h, tag) in listOf(7 to "morning", 12 to "day", 17 to "evening", 22 to "night")) {
                draw(W.build(ctx, com.koussa.islamicazkar.widget.WidgetSet.TIMELINE, 4, compact = false, now = at(h)), 330, 170, "33_widget_timeline_${tag}_$l")
            }
            draw(W.build(ctx, com.koussa.islamicazkar.widget.WidgetSet.MORNING, 1, compact = true, now = at(8)), 160, 160, "34_widget_small_morning_$l")
            draw(W.build(ctx, com.koussa.islamicazkar.widget.WidgetSet.NIGHT, 3, compact = true, now = at(23)), 160, 160, "35_widget_small_night_$l")
        }
        // Picker previews (transparent around the widget).
        Settings.get(ctx).updateLang(Lang.AR)
        draw(W.build(ctx, com.koussa.islamicazkar.widget.WidgetSet.MORNING, 11, compact = false, now = at(6)), 330, 170, "widget_preview_morning", wallpaper = false)
        draw(W.build(ctx, com.koussa.islamicazkar.widget.WidgetSet.DAY, 12, compact = false, now = at(13)), 330, 170, "widget_preview_day", wallpaper = false)
        draw(W.build(ctx, com.koussa.islamicazkar.widget.WidgetSet.NIGHT, 13, compact = false, now = at(21)), 330, 170, "widget_preview_night", wallpaper = false)
        draw(W.build(ctx, com.koussa.islamicazkar.widget.WidgetSet.TIMELINE, 14, compact = false, now = at(17)), 330, 170, "widget_preview_timeline", wallpaper = false)
    }

    @Test fun islamicWidgets() {
        val dm = ctx.resources.displayMetrics
        fun draw(v: android.widget.RemoteViews, wDp: Int, hDp: Int, name: String, wallpaper: Boolean = true) {
            val view = v.apply(ctx, android.widget.FrameLayout(ctx))
            val w = (wDp * dm.density).toInt(); val h = (hDp * dm.density).toInt()
            view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, w, h)
            val pad = (12 * dm.density).toInt()
            val bmp = Bitmap.createBitmap(w + pad * 2, h + pad * 2, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val p = android.graphics.Paint().apply {
                shader = android.graphics.LinearGradient(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat(),
                    intArrayOf(0xFF5B7FA6.toInt(), 0xFFB98A6E.toInt(), 0xFF3E6B5A.toInt()), null, android.graphics.Shader.TileMode.CLAMP)
            }
            if (wallpaper) c.drawRect(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat(), p)
            c.translate(pad.toFloat(), pad.toFloat())
            view.draw(c)
            File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        // A sample week of tasbih for the statistics chart.
        val sp = Settings.prefs(ctx)
        val now = System.currentTimeMillis()
        val today = (now + java.util.TimeZone.getDefault().getOffset(now)) / 86_400_000L
        listOf(120, 66, 0, 233, 99, 165, 39).forEachIndexed { i, n -> sp.edit().putInt("tasbih_day_${today - 6 + i}", n).commit() }
        sp.edit().putInt(Settings.K_TASBIH_COUNT, 39).commit()
        val hour = 3_600_000L
        val ramadan = java.util.Calendar.getInstance().apply { set(2027, java.util.Calendar.FEBRUARY, 19) }
        val P = com.koussa.islamicazkar.widget.ScriptureWidget
        for (lang in listOf(Lang.AR, Lang.EN, Lang.FR)) {
            Settings.get(ctx).updateLang(lang)
            val l = lang.code
            sp.edit().putInt("cards_cat_21", 0).putInt("cards_cat_22", 3).putInt("cards_cat_23", 2).commit()
            draw(com.koussa.islamicazkar.widget.AzkarCardsWidget.views(ctx, 21, 330 to 230), 330, 230, "40_widget_cards_$l")
            draw(com.koussa.islamicazkar.widget.AzkarCardsWidget.views(ctx, 22, 330 to 230), 330, 230, "40_widget_cards_prayer_$l")
            draw(com.koussa.islamicazkar.widget.AzkarCardsWidget.views(ctx, 23, 160 to 160), 160, 160, "41_widget_cards_small_$l")
            sp.edit().putInt("quote_src_31", 1).putInt("quote_src_32", 2).putInt("quote_src_34", 4).putInt("quote_src_35", 5).putInt("quote_src_36", 0).commit()
            draw(P.views(ctx, 31, 330 to 170, now = 5 * hour), 330, 170, "42_widget_verse_$l")
            draw(P.views(ctx, 32, 330 to 170, now = 7 * hour), 330, 170, "42_widget_hadith_$l")
            draw(P.views(ctx, 34, 330 to 170, now = 3 * hour), 330, 170, "42_widget_poetry_$l")
            draw(P.views(ctx, 35, 330 to 170, now = 4 * hour), 330, 170, "42_widget_wisdom_$l")
            draw(P.views(ctx, 36, 160 to 160, now = 9 * hour), 160, 160, "42_widget_quote_small_$l")
            draw(P.views(ctx, 33, 330 to 70, now = 2 * hour), 330, 70, "43_widget_verse_strip_$l")
            draw(com.koussa.islamicazkar.widget.CounterStatsWidget.views(ctx, 41, 330 to 170), 330, 170, "44_widget_counter_$l")
            draw(com.koussa.islamicazkar.widget.CounterStatsWidget.views(ctx, 42, 160 to 160), 160, 160, "45_widget_counter_small_$l")
            draw(com.koussa.islamicazkar.widget.HijriDateWidget.views(ctx, 51, 160 to 160, ramadan), 160, 160, "46_widget_hijri_$l")
            draw(com.koussa.islamicazkar.widget.HijriDateWidget.views(ctx, 52, 330 to 160, ramadan), 330, 160, "47_widget_hijri_wide_$l")
        }
        draw(com.koussa.islamicazkar.widget.IslamicClockWidget.views(ctx, 71), 170, 170, "48_widget_clock")

        // Picker previews (transparent around the widget).
        Settings.get(ctx).updateLang(Lang.AR)
        sp.edit().putInt("cards_cat_61", 0).commit()
        draw(com.koussa.islamicazkar.widget.AzkarCardsWidget.views(ctx, 61, 330 to 230), 330, 230, "widget_preview_cards", wallpaper = false)
        sp.edit().putInt("quote_src_62", 4).commit()
        draw(P.views(ctx, 62, 330 to 170, now = 5 * hour), 330, 170, "widget_preview_scripture", wallpaper = false)
        draw(com.koussa.islamicazkar.widget.CounterStatsWidget.views(ctx, 63, 330 to 170), 330, 170, "widget_preview_counter", wallpaper = false)
        draw(com.koussa.islamicazkar.widget.HijriDateWidget.views(ctx, 64, 160 to 160, ramadan), 160, 160, "widget_preview_hijri", wallpaper = false)
        draw(com.koussa.islamicazkar.widget.IslamicClockWidget.views(ctx, 72), 160, 160, "widget_preview_clock", wallpaper = false)
    }

    @Test fun moreWidgets() {
        val dm = ctx.resources.displayMetrics
        val activity = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        fun draw(v: android.widget.RemoteViews, wDp: Int, hDp: Int, name: String, wallpaper: Boolean = true) {
            // Attach to a real window so live views (TextClock, Chronometer) fill in their text.
            val host = android.widget.FrameLayout(activity)
            val view = v.apply(activity, host)
            host.addView(view)
            activity.setContentView(host)
            val w = (wDp * dm.density).toInt(); val h = (hDp * dm.density).toInt()
            view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, w, h)
            val pad = (12 * dm.density).toInt()
            val bmp = Bitmap.createBitmap(w + pad * 2, h + pad * 2, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val p = android.graphics.Paint().apply {
                shader = android.graphics.LinearGradient(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat(),
                    intArrayOf(0xFF5B7FA6.toInt(), 0xFFB98A6E.toInt(), 0xFF3E6B5A.toInt()), null, android.graphics.Shader.TileMode.CLAMP)
            }
            if (wallpaper) c.drawRect(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat(), p)
            c.translate(pad.toFloat(), pad.toFloat())
            view.draw(c)
            File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val st = Settings.get(ctx)
        val W = com.koussa.islamicazkar.widget.PrayerTimesWidget
        val D = com.koussa.islamicazkar.widget.DigitalClockWidget
        val G = com.koussa.islamicazkar.widget.ProgressWidget
        val H = com.koussa.islamicazkar.widget.HijriDateWidget
        val Q = com.koussa.islamicazkar.widget.ScriptureWidget
        val beirut = com.koussa.islamicazkar.data.Cities.all.first { it.name == "Beirut" }
        // 14:05 in Beirut on 31 October 2026: Asr is next.
        val now = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Beirut")).apply { set(2026, 9, 31, 14, 5, 0) }
        val t = now.timeInMillis
        st.updatePalette(0); st.updateScript(0); st.updateHijriOffset(0)
        st.updatePlace(null)
        st.updateLang(Lang.AR)
        draw(W.views(ctx, 80, 160 to 160, now = t), 160, 160, "50_prayer_noplace_ar")
        st.updatePlace(beirut)
        for (lang in listOf(Lang.AR, Lang.EN, Lang.FR)) {
            st.updateLang(lang)
            val l = lang.code
            draw(W.views(ctx, 81, 330 to 170, now = t), 330, 170, "51_prayer_medium_$l")
            draw(W.views(ctx, 82, 330 to 340, now = t), 330, 340, "52_prayer_large_$l")
            draw(W.views(ctx, 83, 160 to 160, now = t), 160, 160, "53_prayer_small_$l")
            draw(W.views(ctx, 84, 330 to 72, now = t), 330, 72, "54_prayer_strip_$l")
            draw(W.views(ctx, 85, 76 to 76, now = t), 76, 76, "55_prayer_ring_$l")
            draw(D.views(ctx, 86, 330 to 170, now), 330, 170, "56_digital_$l")
            draw(D.views(ctx, 87, 330 to 72, now), 330, 72, "57_digital_strip_$l")
            draw(D.views(ctx, 88, 160 to 160, now), 160, 160, "57_digital_small_$l")
            draw(G.views(ctx, 89, 330 to 170, now), 330, 170, "58_progress_$l")
            draw(G.views(ctx, 90, 160 to 160, now), 160, 160, "58_progress_small_$l")
            draw(G.views(ctx, 91, 330 to 72, now), 330, 72, "59_progress_strip_$l")
            draw(H.views(ctx, 92, 330 to 72, now), 330, 72, "60_hijri_inline_$l")
            Settings.prefs(ctx).edit().putInt("quote_src_93", 0).commit()
            draw(Q.views(ctx, 93, 330 to 72, now = 2 * 3_600_000L), 330, 72, "61_quote_strip_$l")
        }
        // The four calligraphy styles on the Hijri date and the prayer names.
        st.updateLang(Lang.AR)
        for (sc in com.koussa.islamicazkar.data.Script.entries) {
            st.updateScript(sc.ordinal)
            draw(H.views(ctx, 94, 160 to 160, now), 160, 160, "62_script_${sc.name.lowercase()}_date")
            draw(W.views(ctx, 95, 330 to 170, now = t), 330, 170, "62_script_${sc.name.lowercase()}_prayer")
            Settings.prefs(ctx).edit().putInt("quote_src_96", 5).commit()
            draw(Q.views(ctx, 96, 330 to 170, now = 6 * 3_600_000L), 330, 170, "62_script_${sc.name.lowercase()}_wisdom")
        }
        st.updateScript(0)
        // Palettes.
        for (pal in com.koussa.islamicazkar.data.Palette.entries) {
            st.updatePalette(pal.ordinal)
            draw(W.views(ctx, 97, 330 to 170, now = t), 330, 170, "63_palette_${pal.name.lowercase()}")
            draw(com.koussa.islamicazkar.widget.IslamicClockWidget.views(ctx, 98), 160, 160, "64_clock_${pal.name.lowercase()}")
        }
        st.updatePalette(0)
        // Picker previews.
        draw(W.views(ctx, 99, 330 to 170, now = t), 330, 170, "widget_preview_prayer", wallpaper = false)
        draw(D.views(ctx, 100, 330 to 170, now), 330, 170, "widget_preview_digital", wallpaper = false)
        draw(G.views(ctx, 101, 330 to 170, now), 330, 170, "widget_preview_progress", wallpaper = false)
        st.updatePlace(null)
    }

    @Test fun widgets() {
        val dm = ctx.resources.displayMetrics
        fun render(layout: Int, wDp: Int, hDp: Int, name: String, fill: (View) -> Unit = {}) {
            val v = LayoutInflater.from(ctx).inflate(layout, null)
            fill(v)
            val w = (wDp * dm.density).toInt(); val h = (hDp * dm.density).toInt()
            v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            v.layout(0, 0, w, h)
            val bmp = Bitmap.createBitmap(w + 40, h + 40, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(0xFF6F8FA8.toInt()) // stand-in wallpaper
            c.translate(20f, 20f)
            v.draw(c)
            File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val z = AzkarRepository.get(ctx).morning[13]
        render(R.layout.widget_dhikr, 320, 170, "12_widget_dhikr") {
            it.findViewById<TextView>(R.id.w_arabic).text = z.ar
            it.findViewById<TextView>(R.id.w_meaning).text = z.en
            it.findViewById<TextView>(R.id.w_title).text = "Dhikr of the day"
        }
        render(R.layout.widget_quick, 320, 80, "13_widget_quick")
        render(R.layout.widget_tasbih, 160, 160, "14_widget_tasbih")
        // Picker / gallery previews (transparent around the widget), in Arabic like the others.
        fun preview(layout: Int, wDp: Int, hDp: Int, name: String, fill: (View) -> Unit = {}) {
            val v = LayoutInflater.from(ctx).inflate(layout, null)
            fill(v)
            val w = (wDp * dm.density).toInt(); val h = (hDp * dm.density).toInt()
            v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            v.layout(0, 0, w, h)
            val pad = (12 * dm.density).toInt()
            val bmp = Bitmap.createBitmap(w + pad * 2, h + pad * 2, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.translate(pad.toFloat(), pad.toFloat())
            v.draw(c)
            File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val s = com.koussa.islamicazkar.i18n.strings(Lang.AR)
        preview(R.layout.widget_dhikr, 330, 170, "widget_preview_dhikr") {
            it.layoutDirection = View.LAYOUT_DIRECTION_RTL
            it.findViewById<TextView>(R.id.w_arabic).text = z.ar
            it.findViewById<TextView>(R.id.w_meaning).visibility = View.GONE
            it.findViewById<TextView>(R.id.w_title).text = s.widgetDhikr
        }
        preview(R.layout.widget_quick, 330, 90, "widget_preview_quick") {
            it.layoutDirection = View.LAYOUT_DIRECTION_RTL
            it.findViewById<TextView>(R.id.q_morning_label).text = s.morningAzkar
            it.findViewById<TextView>(R.id.q_evening_label).text = s.eveningAzkar
            it.findViewById<TextView>(R.id.q_sleep_label).text = s.sleepAzkar
        }
        preview(R.layout.widget_tasbih, 160, 160, "widget_preview_tasbih") {
            it.findViewById<TextView>(R.id.t_count).text = "٣٣"
            it.findViewById<TextView>(R.id.t_target).text = "من ٣٣"
            it.findViewById<android.widget.ProgressBar>(R.id.t_progress).progress = 33
        }
    }
}
