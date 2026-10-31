package com.koussa.islamicazkar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.icu.util.IslamicCalendar
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import com.koussa.islamicazkar.MainActivity
import com.koussa.islamicazkar.R
import com.koussa.islamicazkar.data.AzkarRepository
import com.koussa.islamicazkar.data.Hadiths
import com.koussa.islamicazkar.data.Poetry
import com.koussa.islamicazkar.data.Wisdom
import com.koussa.islamicazkar.i18n.more
import com.koussa.islamicazkar.data.Hijri
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Palette
import com.koussa.islamicazkar.data.Script
import com.koussa.islamicazkar.data.digits
import com.koussa.islamicazkar.data.Settings
import com.koussa.islamicazkar.data.TasbihHistory
import com.koussa.islamicazkar.data.TasbihPhrases
import com.koussa.islamicazkar.data.Verses
import com.koussa.islamicazkar.data.Zikr
import com.koussa.islamicazkar.i18n.num
import com.koussa.islamicazkar.i18n.strings
import java.util.Calendar
import java.util.Locale

// Islamic widgets (azkar cards with categories, Quran/Hadith,
// counter with weekly stats, Hijri date, Islamic clock), drawn in the app's Mushaf glass style.

private const val GOLD = 0xFFE6C676.toInt()
private const val GOLD_SOFT = 0xCCE6C676.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
private const val WHITE_SOFT = 0xB8FFFFFF.toInt()

/** Widget size in dp from the launcher's options (portrait: min width × max height). */
internal fun widgetSize(ctx: Context, id: Int, defW: Int, defH: Int): Pair<Int, Int> {
    val o: Bundle = AppWidgetManager.getInstance(ctx).getAppWidgetOptions(id) ?: Bundle()
    val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
    val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
    return (if (w > 0) w else defW) to (if (h > 0) h else defH)
}

internal fun RemoteViews.direction(id: Int, lang: Lang) =
    setInt(id, "setLayoutDirection", if (lang == Lang.AR) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR)

internal fun selfIntent(ctx: Context, cls: Class<*>, action: String, id: Int, code: Int, extra: Int = 0): PendingIntent =
    PendingIntent.getBroadcast(
        ctx, id * 100 + code,
        Intent(ctx, cls).setAction(action).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).putExtra("arg", extra),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

internal fun openApp(ctx: Context, code: Int, open: String? = null, focus: Int? = null, tab: Int? = null): PendingIntent {
    val i = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    open?.let { i.putExtra(MainActivity.EXTRA_OPEN, it) }
    focus?.let { i.putExtra(MainActivity.EXTRA_FOCUS, it) }
    tab?.let { i.putExtra(MainActivity.EXTRA_TAB, it) }
    return PendingIntent.getActivity(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

/** Draws text and small artwork into bitmaps so widgets can use the app's own typefaces. */
object WidgetArt {
    fun font(ctx: Context, res: Int, fallback: Typeface = Typeface.SERIF): Typeface = ResourcesCompat.getFont(ctx, res) ?: fallback

    /** The largest size (≤ [maxSp]) at which [text] fits the box, rendered centred. */
    fun text(
        ctx: Context, text: String, typeface: Typeface, color: Int, wDp: Int, hDp: Int,
        maxSp: Float, minSp: Float = 10f, rtl: Boolean = true, spacing: Float = 1.25f,
    ): Bitmap {
        val d = ctx.resources.displayMetrics.density
        val scale = minOf(1f, 900f / (wDp * d))  // keep bitmaps within widget memory limits
        val w = (wDp * d * scale).toInt().coerceAtLeast(1)
        val h = (hDp * d * scale).toInt().coerceAtLeast(1)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { this.typeface = typeface; this.color = color }
        var size = maxSp
        var layout: StaticLayout
        while (true) {
            paint.textSize = size * d * scale * ctx.resources.configuration.fontScale.coerceAtMost(1.15f)
            layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, w)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setTextDirection(if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .setLineSpacing(0f, spacing).setIncludePad(false).build()
            if (layout.height <= h) break
            if (size <= minSp) {
                val lineH = layout.height / layout.lineCount.coerceAtLeast(1)
                layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, w)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setTextDirection(if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR)
                    .setLineSpacing(0f, spacing).setIncludePad(false)
                    .setMaxLines((h / lineH.coerceAtLeast(1)).coerceAtLeast(1)).setEllipsize(android.text.TextUtils.TruncateAt.END).build()
                break
            }
            size -= 1f
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.translate(0f, ((h - layout.height) / 2f).coerceAtLeast(0f))
        layout.draw(c)
        return bmp
    }

    /** Seven bars for the week, today in gold, with the day initials underneath. */
    fun weekChart(ctx: Context, days: List<Pair<Long, Int>>, lang: Lang, wDp: Int, hDp: Int): Bitmap {
        val d = ctx.resources.displayMetrics.density
        val w = (wDp * d).toInt().coerceIn(1, 900)
        val h = (hDp * d).toInt().coerceIn(1, 600)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val label = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10 * d; color = WHITE_SOFT; textAlign = Paint.Align.CENTER; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
        val value = TextPaint(label).apply { color = WHITE; textSize = 9.5f * d }
        val max = (days.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
        val n = days.size
        val slot = w / n.toFloat()
        val barW = slot * 0.56f
        val top = 16 * d
        val bottom = h - 16 * d
        val names = weekdayInitials(lang)
        val ordered = if (lang == Lang.AR) days.reversed() else days  // right-to-left in Arabic
        ordered.forEachIndexed { i, (day, count) ->
            val cx = slot * i + slot / 2
            val isToday = day == days.last().first
            val bh = ((bottom - top) * count / max).coerceAtLeast(3 * d)
            val r = RectF(cx - barW / 2, bottom - bh, cx + barW / 2, bottom)
            p.shader = if (isToday) LinearGradient(0f, r.top, 0f, r.bottom, 0xFFF1DBA3.toInt(), 0xFFC79C4E.toInt(), Shader.TileMode.CLAMP) else null
            p.color = if (isToday) GOLD else 0x40FFFFFF
            c.drawRoundRect(r, barW / 3, barW / 3, p)
            if (count > 0) c.drawText(lang.num(count), cx, r.top - 3 * d, value)
            val dow = dayOfWeek(day)
            label.color = if (isToday) GOLD else WHITE_SOFT
            label.textSize = (if (lang == Lang.AR) 12f else 10f) * d
            c.drawText(names[dow], cx, h - 3 * d, label)
        }
        return bmp
    }

    private fun dayOfWeek(epochDay: Long): Int = Math.floorMod((epochDay + 4), 7L).toInt() // 0 = Sunday

    private fun weekdayInitials(lang: Lang) = when (lang) {
        Lang.AR -> listOf("ح", "ن", "ث", "ر", "خ", "ج", "س")
        Lang.EN -> listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        Lang.FR -> listOf("DIM", "LUN", "MAR", "MER", "JEU", "VEN", "SAM")
    }

    /** A bitmap sized for a [wDp]×[hDp] widget area, capped to keep within widget memory limits. */
    fun canvas(ctx: Context, wDp: Int, hDp: Int, maxW: Int = 900, maxH: Int = 700): Triple<Bitmap, Canvas, Float> {
        val d = ctx.resources.displayMetrics.density
        val scale = minOf(1f, maxW / (wDp * d), maxH / (hDp * d))
        val bmp = Bitmap.createBitmap((wDp * d * scale).toInt().coerceAtLeast(1), (hDp * d * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        return Triple(bmp, Canvas(bmp), d * scale)
    }

    fun paint(tf: Typeface, sizePx: Float, color: Int, align: Paint.Align = Paint.Align.CENTER) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tf; textSize = sizePx; this.color = color; textAlign = align }

    fun fit(p: TextPaint, text: String, maxW: Float) { while (p.measureText(text) > maxW && p.textSize > 6f) p.textSize *= 0.94f }

    /** How large each script draws relative to its size: Nastaʿlīq and Kufic are tall. */
    fun scriptScale(s: Script) = when (s) { Script.NASTALIQ -> 0.72f; Script.KUFI -> 0.86f; Script.NASKH -> 0.92f; Script.RUQAA -> 1f }

    /** Draws [parts] (in reading order) on one line, centred, shrinking them together to fit [maxW]. */
    fun line(c: Canvas, parts: List<Pair<String, TextPaint>>, rtl: Boolean, cx: Float, baseline: Float, maxW: Float, gap: Float) {
        val visual = if (rtl) parts.reversed() else parts
        visual.forEach { it.second.textAlign = Paint.Align.LEFT }
        fun width() = visual.sumOf { it.second.measureText(it.first).toDouble() }.toFloat() + gap * (visual.size - 1)
        // Calligraphic flourishes reach past a glyph's advance, so keep a margin on both sides.
        val room = maxW * 0.92f
        val w0 = width()
        if (w0 > room) visual.forEach { it.second.textSize *= room / w0 }
        var x = cx - width() / 2
        for ((t, p) in visual) { c.drawText(t, x, baseline, p); x += p.measureText(t) + gap }
    }

    /**
     * The Hijri date as artwork: weekday and month in the chosen calligraphy, the day large and
     * the Gregorian date underneath. Wide sizes put the two side by side; slim ones use one line.
     */
    fun hijri(ctx: Context, lang: Lang, wDp: Int, hDp: Int, now: Calendar = Calendar.getInstance()): Bitmap {
        val (bmp, c, d) = canvas(ctx, wDp, hDp)
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        val script = Script.current(ctx)
        val cal = script.typeface(ctx)
        val k = scriptScale(script)
        val amiri = font(ctx, R.font.amiri_bold)
        val sans = font(ctx, R.font.inter, Typeface.DEFAULT)

        val hd = Hijri.of(ctx, now.timeInMillis)
        val dow = now.get(Calendar.DAY_OF_WEEK) - 1
        val weekday = Hijri.weekdayAr(dow)
        val month = Hijri.monthAr(hd.month)
        val loc = Locale(lang.code)
        val yearLine = if (lang == Lang.AR) "${Lang.AR.digits(hd.year)} هـ" else Hijri.format(lang, hd).substringAfter(' ')

        when {
            hDp < 90 -> {
                // One line, for slim widgets and the lock screen.
                val greg = java.text.SimpleDateFormat(if (lang == Lang.AR) "d MMMM" else "EEE d MMM", loc).format(now.time)
                val parts = if (lang == Lang.AR) listOf(
                    weekday to paint(cal, h * 0.50f * k, GOLD),
                    Lang.AR.digits(hd.day) to paint(amiri, h * 0.42f, WHITE),
                    month to paint(cal, h * 0.46f * k, GOLD),
                    yearLine to paint(sans, h * 0.24f, WHITE_SOFT),
                    greg to paint(sans, h * 0.24f, WHITE_SOFT),
                ) else listOf(
                    weekday to paint(cal, h * 0.50f * k, GOLD),
                    Hijri.format(lang, hd) to paint(sans, h * 0.28f, WHITE),
                    "· $greg" to paint(sans, h * 0.24f, WHITE_SOFT),
                )
                line(c, parts, lang == Lang.AR, w / 2, h * 0.66f, w * 0.96f, 9 * d)
            }
            wDp < 230 -> {
                val cx = w / 2
                val wd = paint(cal, h * 0.15f * k, GOLD_SOFT); fit(wd, weekday, w * 0.9f)
                c.drawText(weekday, cx, h * 0.21f, wd)
                val day = paint(amiri, h * 0.36f, WHITE)
                c.drawText(Lang.AR.digits(hd.day), cx, h * 0.55f, day)
                val mp = paint(cal, h * 0.18f * k, GOLD); fit(mp, month, w * 0.92f)
                c.drawText(month, cx, h * 0.77f, mp)
                val small = paint(sans, 10.5f * d, WHITE_SOFT)
                val tail = if (lang == Lang.AR) yearLine else Hijri.format(lang, hd).substringAfter(' ')
                fit(small, tail, w * 0.92f)
                c.drawText(tail, cx, h * 0.93f, small)
            }
            else -> {
                // Day + month on one side, weekday and Gregorian on the other (mirrored for Arabic).
                val greg = java.text.SimpleDateFormat("EEEE d MMMM yyyy", loc).format(now.time)
                val (dayX, infoX) = if (lang == Lang.AR) w * 0.72f to w * 0.27f else w * 0.27f to w * 0.71f
                val day = paint(amiri, h * 0.42f, WHITE)
                c.drawText(Lang.AR.digits(hd.day), dayX, h * 0.55f, day)
                val mp = paint(cal, h * 0.19f * k, GOLD); fit(mp, month, w * 0.42f)
                c.drawText(month, dayX, h * 0.85f, mp)
                val wd = paint(cal, h * 0.28f * k, GOLD); fit(wd, weekday, w * 0.44f)
                c.drawText(weekday, infoX, h * 0.42f, wd)
                val g = paint(sans, 11f * d, WHITE); fit(g, greg, w * 0.46f)
                c.drawText(greg, infoX, h * 0.63f, g)
                val y = paint(sans, 10.5f * d, WHITE_SOFT)
                val yl = yearLine
                fit(y, yl, w * 0.46f)
                c.drawText(yl, infoX, h * 0.79f, y)
                val dp = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, h * 0.2f, 0f, h * 0.8f, intArrayOf(0, GOLD_SOFT, 0), null, Shader.TileMode.CLAMP) }
                c.drawRect(w * 0.485f, h * 0.2f, w * 0.485f + d, h * 0.8f, dp)
            }
        }
        return bmp
    }

    private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float): android.graphics.Path = android.graphics.Path().apply {
        for (i in 0 until 16) {
            val r = if (i % 2 == 0) outer else inner
            val a = Math.toRadians(i * 22.5 - 90)
            val x = cx + r * Math.cos(a).toFloat()
            val y = cy + r * Math.sin(a).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    /** A clock face in the palette's colours: eight-pointed star, minute marks and Arabic-Indic numerals. */
    fun dial(ctx: Context, palette: Palette, px: Int = 480): Bitmap {
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val r = px / 2f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = 0x38FFFFFF
        c.drawCircle(r, r, r, p)
        val face = android.graphics.Path().apply { addCircle(r, r, r * 0.97f, android.graphics.Path.Direction.CW) }
        p.color = WHITE  // a shader is multiplied by the paint's alpha
        p.shader = LinearGradient(0f, 0f, 0f, px.toFloat(), (palette.top and 0x00FFFFFF) or (0xF0 shl 24), (palette.bottom and 0x00FFFFFF) or (0xF0 shl 24), Shader.TileMode.CLAMP)
        c.drawPath(face, p)
        p.shader = LinearGradient(0f, 0f, 0f, px * 0.6f, 0x30FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        c.drawPath(face, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = px * 0.004f
        p.color = 0x66F1DBA3
        c.drawCircle(r, r, r * 0.92f, p)

        val star = starPath(r, r, r * 0.56f, r * 0.43f)
        p.style = Paint.Style.FILL; p.color = 0x24D6B36A; c.drawPath(star, p)
        p.style = Paint.Style.STROKE; p.color = 0xCCD6B36A.toInt(); p.strokeWidth = px * 0.006f; c.drawPath(star, p)
        p.color = 0x59F1DBA3; p.strokeWidth = px * 0.004f; c.drawPath(starPath(r, r, r * 0.38f, r * 0.29f), p)
        c.drawCircle(r, r, r * 0.2f, p)

        p.style = Paint.Style.FILL
        for (i in 0 until 60) {
            val major = i % 5 == 0
            val a = Math.toRadians(i * 6.0 - 90)
            val r1 = r * 0.92f; val r2 = r * if (major) 0.85f else 0.88f
            p.color = if (major) GOLD else 0x99FFFFFF.toInt()
            p.strokeWidth = px * if (major) 0.008f else 0.004f
            c.drawLine(r + r1 * Math.cos(a).toFloat(), r + r1 * Math.sin(a).toFloat(), r + r2 * Math.cos(a).toFloat(), r + r2 * Math.sin(a).toFloat(), p)
        }
        val num = paint(font(ctx, R.font.amiri_bold), px * 0.085f, WHITE)
        for (hr in 1..12) {
            val a = Math.toRadians(hr * 30.0 - 90)
            num.color = if (hr % 3 == 0) GOLD else 0xE6FFFFFF.toInt()
            num.textSize = px * if (hr % 3 == 0) 0.095f else 0.07f
            val x = r + r * 0.71f * Math.cos(a).toFloat()
            val y = r + r * 0.71f * Math.sin(a).toFloat() - (num.ascent() + num.descent()) / 2
            c.drawText(Lang.AR.digits(hr), x, y, num)
        }
        return bmp
    }

    /** A progress ring with a value in the middle and an optional label underneath. */
    fun ring(c: Canvas, cx: Float, cy: Float, radius: Float, fraction: Float, stroke: Float, center: String, centerPaint: TextPaint, label: String?, labelPaint: TextPaint?) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND }
        val box = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        p.color = 0x2EFFFFFF
        c.drawPath(android.graphics.Path().apply { addOval(box, android.graphics.Path.Direction.CW) }, p)
        p.color = GOLD
        c.drawPath(android.graphics.Path().apply { addArc(box, -90f, 360f * fraction.coerceIn(0.002f, 1f)) }, p)
        centerPaint.textAlign = Paint.Align.CENTER
        c.drawText(center, cx, cy - (centerPaint.ascent() + centerPaint.descent()) / 2, centerPaint)
        if (label != null && labelPaint != null) {
            labelPaint.textAlign = Paint.Align.CENTER
            c.drawText(label, cx, cy + radius + stroke + labelPaint.textSize * 1.1f, labelPaint)
        }
    }
}

/** Short labels used only by these widgets. */
private object WL {
    fun categories(lang: Lang): List<String> { val s = strings(lang); return listOf(s.morningAzkar, s.eveningAzkar, s.sleepAzkar, s.afterPrayer, s.dayAzkar) }
    fun quran(lang: Lang) = when (lang) { Lang.AR -> "القرآن الكريم"; Lang.EN -> "Quran"; Lang.FR -> "Coran" }
    fun hadith(lang: Lang) = when (lang) { Lang.AR -> "الحديث الشريف"; Lang.EN -> "Hadith"; Lang.FR -> "Hadith" }
    fun counter(lang: Lang) = when (lang) { Lang.AR -> "العدّاد"; Lang.EN -> "Counter"; Lang.FR -> "Compteur" }
    fun weekly(lang: Lang) = when (lang) { Lang.AR -> "إحصاء الأسبوع"; Lang.EN -> "Weekly stats"; Lang.FR -> "Cette semaine" }
    fun total(lang: Lang, n: Int) = when (lang) { Lang.AR -> "المجموع: ${lang.num(n)}"; Lang.EN -> "Total: $n"; Lang.FR -> "Total : $n" }
}

// ───────────────────────── Azkar Cards (with categories) ─────────────────────────

/** Azkar with a category bar (morning · evening · sleep · prayer · day); tap the pill for the next one. */
class AzkarCardsWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = ids.forEach { mgr.updateAppWidget(it, views(ctx, it)) }
    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, o: Bundle) = mgr.updateAppWidget(id, views(ctx, id))

    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        val sp = Settings.prefs(ctx)
        when (intent.action) {
            ACTION_NEXT -> sp.edit().putInt("cards_idx_$id", sp.getInt("cards_idx_$id", 0) + 1).apply()
            ACTION_CATEGORY -> sp.edit().putInt("cards_cat_$id", intent.getIntExtra("arg", 0)).putInt("cards_idx_$id", 0).apply()
            else -> return super.onReceive(ctx, intent)
        }
        AppWidgetManager.getInstance(ctx).updateAppWidget(id, views(ctx, id))
    }

    companion object {
        private const val ACTION_NEXT = "com.koussa.islamicazkar.widget.CARDS_NEXT"
        private const val ACTION_CATEGORY = "com.koussa.islamicazkar.widget.CARDS_CATEGORY"
        private val ICONS = listOf(R.drawable.ic_sun, R.drawable.ic_moon, R.drawable.ic_bed, R.drawable.ic_mosque, R.drawable.ic_day)
        private val BUTTONS = listOf(R.id.c_cat0, R.id.c_cat1, R.id.c_cat2, R.id.c_cat3, R.id.c_cat4)

        fun items(ctx: Context, cat: Int): List<Zikr> {
            val data = AzkarRepository.get(ctx)
            return when (cat) {
                0 -> data.morning
                1 -> data.evening
                2 -> data.chapterById[28]?.items.orEmpty()
                3 -> data.chapterById[25]?.items.orEmpty()
                else -> AzkarSets.items(ctx, WidgetSet.DAY)
            }
        }

        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            Widgets.ids(ctx, AzkarCardsWidget::class.java).forEach { mgr.updateAppWidget(it, views(ctx, it)) }
        }

        fun views(ctx: Context, id: Int, size: Pair<Int, Int> = widgetSize(ctx, id, 320, 220)): RemoteViews {
            val lang = Settings.get(ctx).lang
            val sp = Settings.prefs(ctx)
            val cat = sp.getInt("cards_cat_$id", if (com.koussa.islamicazkar.ui.currentMoment() == com.koussa.islamicazkar.ui.Moment.EVENING) 1 else 0)
            val (w, h) = size
            val compact = w < 230 || h < 150
            val list = items(ctx, cat).let { all -> if (compact) all.filter { it.ar.length <= 160 }.ifEmpty { all } else all }
            val z = list[Math.floorMod(sp.getInt("cards_idx_$id", 0), list.size)]
            val name = WL.categories(lang)[cat]
            val amiri = WidgetArt.font(ctx, R.font.amiri_regular)
            val v: RemoteViews
            if (compact) {
                // Quote card: dhikr, a gold ❝ and its source.
                v = RemoteViews(ctx.packageName, R.layout.widget_cards_small)
                v.setImageViewBitmap(R.id.c_text, WidgetArt.text(ctx, z.ar, amiri, WHITE, w - 24, h - 58, 22f))
                v.setTextViewText(R.id.c_source, "— $name")
                v.setOnClickPendingIntent(R.id.c_root, selfIntent(ctx, AzkarCardsWidget::class.java, ACTION_NEXT, id, 1))
            } else {
                v = RemoteViews(ctx.packageName, R.layout.widget_cards)
                val meaning = z.meaning(lang)
                val textH = h - 70 - if (meaning.isBlank()) 0 else 34
                v.setImageViewBitmap(R.id.c_text, WidgetArt.text(ctx, z.ar, amiri, WHITE, w - 28, textH, 24f))
                v.setViewVisibility(R.id.c_meaning, if (meaning.isBlank()) View.GONE else View.VISIBLE)
                v.setTextViewText(R.id.c_meaning, meaning)
                v.setTextViewText(R.id.c_next, "$name  ›")
                BUTTONS.forEachIndexed { i, b ->
                    v.setImageViewResource(b, ICONS[i])
                    v.setInt(b, "setBackgroundResource", if (i == cat) R.drawable.widget_chip_selected else R.drawable.widget_pill)
                    v.setOnClickPendingIntent(b, selfIntent(ctx, AzkarCardsWidget::class.java, ACTION_CATEGORY, id, 10 + i, i))
                }
                v.setOnClickPendingIntent(R.id.c_next, selfIntent(ctx, AzkarCardsWidget::class.java, ACTION_NEXT, id, 1))
                val chapter = AzkarRepository.get(ctx).chapterOfZikr[z.id]
                v.setOnClickPendingIntent(R.id.c_text, openApp(ctx, 6000 + id, when (chapter) { -1 -> "morning"; -2 -> "evening"; null -> null; else -> "c$chapter" }, z.id))
            }
            v.direction(R.id.c_root, lang)
            Widgets.glass(ctx, v, R.id.c_root)
            return v
        }
    }
}

// ───────────────────────── Quran · Hadith · Azkar · Poetry · Wisdom ─────────────────────────

/**
 * One widget for Quran verses, hadith, azkar, classical poetry and wisdom. Tap the text for
 * a new one, tap the pill to switch the source ("Mixed" cycles through all five). Changes hourly.
 */
class ScriptureWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = ids.forEach { mgr.updateAppWidget(it, views(ctx, it)) }
    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, o: Bundle) = mgr.updateAppWidget(id, views(ctx, id))

    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        val sp = Settings.prefs(ctx)
        when (intent.action) {
            ACTION_NEXT -> sp.edit().putInt("scripture_off_$id", sp.getInt("scripture_off_$id", 0) + 1).apply()
            ACTION_SOURCE -> sp.edit().putInt("quote_src_$id", (sp.getInt("quote_src_$id", 0) + 1) % SOURCES).putInt("scripture_off_$id", 0).apply()
            else -> return super.onReceive(ctx, intent)
        }
        AppWidgetManager.getInstance(ctx).updateAppWidget(id, views(ctx, id))
    }

    /** What a quote is drawn with: its text, meaning, reference and typeface. */
    class Item(val ar: String, val meaning: String, val ref: String, val kind: Int)

    companion object {
        private const val ACTION_NEXT = "com.koussa.islamicazkar.widget.SCRIPTURE_NEXT"
        private const val ACTION_SOURCE = "com.koussa.islamicazkar.widget.SCRIPTURE_SOURCE"
        const val MIXED = 0
        const val QURAN = 1
        const val HADITH = 2
        const val AZKAR = 3
        const val POETRY = 4
        const val WISDOM = 5
        private const val SOURCES = 6

        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            Widgets.ids(ctx, ScriptureWidget::class.java).forEach { mgr.updateAppWidget(it, views(ctx, it)) }
        }

        /** The [step]th quote from [source]; "Mixed" alternates between the five kinds. */
        fun item(ctx: Context, lang: Lang, source: Int, step: Int, short: Boolean = false): Item {
            val kind = if (source == MIXED) Math.floorMod(step, 5) + 1 else source
            val n = if (source == MIXED) Math.floorDiv(step, 5) else step
            fun <T> pick(list: List<T>, len: (T) -> Int): T {
                val pool = if (short) list.filter { len(it) <= 150 }.ifEmpty { list } else list
                return pool[Math.floorMod(n, pool.size)]
            }
            val s = more(lang)
            return when (kind) {
                QURAN -> pick(Verses.all(ctx)) { it.ar.length }.let { Item(it.ar, it.meaning(lang), it.reference(lang), kind) }
                HADITH -> pick(Hadiths.all(ctx)) { it.ar.length }.let { Item(it.ar, it.meaning(lang), s.sources[HADITH], kind) }
                AZKAR -> pick(AzkarRepository.get(ctx).shortAzkar) { it.ar.length }.let { Item(it.ar, it.meaning(lang), s.sources[AZKAR], kind) }
                POETRY -> pick(Poetry.all) { it.ar.length }.let { Item(it.ar, it.meaning(lang), it.source(lang), kind) }
                else -> pick(Wisdom.all) { it.ar.length }.let { Item(it.ar, it.meaning(lang), it.source(lang), kind) }
            }
        }

        /** Quran in the Uthmani script, hadith and azkar in Naskh, poetry and wisdom in the chosen calligraphy. */
        private fun typeface(ctx: Context, kind: Int): Pair<Typeface, Float> = when (kind) {
            QURAN -> WidgetArt.font(ctx, R.font.amiri_quran) to 1f
            HADITH, AZKAR -> WidgetArt.font(ctx, R.font.amiri_regular) to 1f
            else -> Script.current(ctx).let { it.typeface(ctx) to WidgetArt.scriptScale(it) }
        }

        fun views(ctx: Context, id: Int, size: Pair<Int, Int> = widgetSize(ctx, id, 320, 170), now: Long = System.currentTimeMillis()): RemoteViews {
            val lang = Settings.get(ctx).lang
            val sp = Settings.prefs(ctx)
            val (w, h) = size
            val source = sp.getInt("quote_src_$id", MIXED)
            val step = (now / 3_600_000L).toInt() + sp.getInt("scripture_off_$id", 0)
            val slim = h < 100
            val small = !slim && w < 230
            val it = item(ctx, lang, if (slim && source == MIXED) QURAN else source, step, short = slim || small)
            val (tf, k) = typeface(ctx, it.kind)
            val next = selfIntent(ctx, ScriptureWidget::class.java, ACTION_NEXT, id, 1)

            val v: RemoteViews
            when {
                slim -> {
                    v = RemoteViews(ctx.packageName, R.layout.widget_scripture_slim)
                    v.setImageViewBitmap(R.id.s_text, WidgetArt.text(ctx, it.ar.replace('\n', ' '), tf, WHITE, w - 56, h - 18, 20f * k, minSp = 8f))
                    v.setOnClickPendingIntent(R.id.s_root, next)
                }
                small -> {
                    v = RemoteViews(ctx.packageName, R.layout.widget_cards_small)
                    v.setImageViewBitmap(R.id.c_text, WidgetArt.text(ctx, it.ar, tf, WHITE, w - 24, h - 58, 22f * k))
                    v.setTextViewText(R.id.c_source, "— ${it.ref.ifBlank { more(lang).sources[it.kind] }}")
                    v.setOnClickPendingIntent(R.id.c_root, next)
                }
                else -> {
                    v = RemoteViews(ctx.packageName, R.layout.widget_quotes)
                    val meaning = it.meaning
                    val textH = h - 64 - (if (meaning.isBlank()) 0 else 30) - (if (it.ref.isBlank()) 0 else 18)
                    v.setImageViewBitmap(R.id.s_text, WidgetArt.text(ctx, it.ar, tf, WHITE, w - 28, textH, 25f * k, minSp = 9f, spacing = 1.35f))
                    v.setViewVisibility(R.id.s_meaning, if (meaning.isBlank()) View.GONE else View.VISIBLE)
                    v.setTextViewText(R.id.s_meaning, meaning)
                    v.setTextViewText(R.id.s_ref, it.ref)
                    v.setViewVisibility(R.id.s_ref, if (it.ref.isBlank()) View.GONE else View.VISIBLE)
                    val label = more(lang).sources[source] + if (source == MIXED) " · " + more(lang).sources[it.kind] else ""
                    v.setTextViewText(R.id.s_source, label)
                    v.setOnClickPendingIntent(R.id.s_source, selfIntent(ctx, ScriptureWidget::class.java, ACTION_SOURCE, id, 2))
                    v.setOnClickPendingIntent(R.id.s_next, next)
                    v.setOnClickPendingIntent(R.id.s_text, next)
                    v.setImageViewResource(R.id.s_next, if (lang == Lang.AR) R.drawable.ic_chevron_left else R.drawable.ic_chevron_right)
                }
            }
            v.direction(if (small) R.id.c_root else R.id.s_root, lang)
            Widgets.glass(ctx, v, if (small) R.id.c_root else R.id.s_root)
            return v
        }
    }
}

// ───────────────────────── Counter with weekly stats ─────────────────────────

/** A tasbih counter with + and reset, and (when wide) a seven-day bar chart. */
class CounterStatsWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = ids.forEach { mgr.updateAppWidget(it, views(ctx, it)) }
    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, o: Bundle) = mgr.updateAppWidget(id, views(ctx, id))

    override fun onReceive(ctx: Context, intent: Intent) {
        val sp = Settings.prefs(ctx)
        when (intent.action) {
            ACTION_ADD -> {
                sp.edit().putInt(Settings.K_TASBIH_COUNT, sp.getInt(Settings.K_TASBIH_COUNT, 0) + 1)
                    .putInt(Settings.K_TASBIH_TOTAL, sp.getInt(Settings.K_TASBIH_TOTAL, 0) + 1).commit()
                TasbihHistory.add(ctx)
            }
            ACTION_RESET -> sp.edit().putInt(Settings.K_TASBIH_COUNT, 0).commit()
            else -> return super.onReceive(ctx, intent)
        }
        Settings.get(ctx).reloadTasbih()
        refresh(ctx)
        TasbihWidget.refresh(ctx)
    }

    companion object {
        private const val ACTION_ADD = "com.koussa.islamicazkar.widget.COUNTER_ADD"
        private const val ACTION_RESET = "com.koussa.islamicazkar.widget.COUNTER_RESET"

        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            Widgets.ids(ctx, CounterStatsWidget::class.java).forEach { mgr.updateAppWidget(it, views(ctx, it)) }
        }

        fun views(ctx: Context, id: Int, size: Pair<Int, Int> = widgetSize(ctx, id, 320, 170)): RemoteViews {
            val lang = Settings.get(ctx).lang
            val sp = Settings.prefs(ctx)
            val (w, h) = size
            val wide = w >= 230
            val count = sp.getInt(Settings.K_TASBIH_COUNT, 0)
            val phrase = sp.getInt(Settings.K_TASBIH_PHRASE, 0).coerceIn(0, TasbihPhrases.ar.lastIndex)
            val v = RemoteViews(ctx.packageName, if (wide) R.layout.widget_counter_wide else R.layout.widget_counter)
            v.setTextViewText(R.id.k_title, WL.counter(lang))
            v.setTextViewText(R.id.k_phrase, TasbihPhrases.ar[phrase])
            v.setTextViewText(R.id.k_count, if (lang == Lang.AR) lang.num(count) else count.toString().padStart(3, '0'))
            v.setOnClickPendingIntent(R.id.k_add, selfIntent(ctx, CounterStatsWidget::class.java, ACTION_ADD, id, 1))
            v.setOnClickPendingIntent(R.id.k_reset, selfIntent(ctx, CounterStatsWidget::class.java, ACTION_RESET, id, 2))
            if (wide) {
                val week = TasbihHistory.lastWeek(ctx)
                v.setTextViewText(R.id.k_weekly, WL.weekly(lang))
                v.setTextViewText(R.id.k_total, WL.total(lang, week.sumOf { it.second }))
                v.setImageViewBitmap(R.id.k_chart, WidgetArt.weekChart(ctx, week, lang, (w * 0.56f).toInt(), h - 56))
                v.setOnClickPendingIntent(R.id.k_chart, openApp(ctx, 6500 + id))
            }
            v.direction(R.id.k_root, lang)
            Widgets.glass(ctx, v, R.id.k_root)
            return v
        }
    }
}

// ───────────────────────── Hijri date ─────────────────────────

/** Today's Hijri date in calligraphy, with the weekday and the Gregorian date; one line when slim. */
class HijriDateWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = ids.forEach { mgr.updateAppWidget(it, views(ctx, it)) }
    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, o: Bundle) = mgr.updateAppWidget(id, views(ctx, id))

    companion object {
        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            Widgets.ids(ctx, HijriDateWidget::class.java).forEach { mgr.updateAppWidget(it, views(ctx, it)) }
        }

        fun views(ctx: Context, id: Int, size: Pair<Int, Int> = widgetSize(ctx, id, 160, 160), now: Calendar = Calendar.getInstance()): RemoteViews {
            val lang = Settings.get(ctx).lang
            val (w, h) = size
            val v = RemoteViews(ctx.packageName, R.layout.widget_hijri)
            v.setImageViewBitmap(R.id.h_art, WidgetArt.hijri(ctx, lang, w - 16, h - 16, now))
            v.setOnClickPendingIntent(R.id.h_root, openApp(ctx, 6800 + id))
            Widgets.glass(ctx, v, R.id.h_root)
            return v
        }
    }
}

// ───────────────────────── Islamic clock ─────────────────────────

/**
 * An analogue clock on an eight-pointed-star dial. From Android 12 the dial is drawn in the
 * chosen palette with Arabic-Indic numerals; the launcher moves the hands live.
 */
class IslamicClockWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = ids.forEach { mgr.updateAppWidget(it, views(ctx, it)) }

    companion object {
        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            Widgets.ids(ctx, IslamicClockWidget::class.java).forEach { mgr.updateAppWidget(it, views(ctx, it)) }
        }

        fun views(ctx: Context, id: Int): RemoteViews {
            val v = RemoteViews(ctx.packageName, R.layout.widget_clock)
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                v.setIcon(R.id.o_clock, "setDial", android.graphics.drawable.Icon.createWithBitmap(WidgetArt.dial(ctx, Palette.current(ctx))))
            }
            v.setOnClickPendingIntent(R.id.o_root, openApp(ctx, 6900 + id))
            return v
        }
    }
}

/** Refreshes every widget in this file (language changes, the 15-minute ticker, midnight). */
object IslamicWidgets {
    fun refreshAll(ctx: Context) {
        AzkarCardsWidget.refresh(ctx)
        ScriptureWidget.refresh(ctx)
        CounterStatsWidget.refresh(ctx)
        HijriDateWidget.refresh(ctx)
        IslamicClockWidget.refresh(ctx)
    }
}
