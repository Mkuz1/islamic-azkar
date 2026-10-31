package com.koussa.islamicazkar.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.icu.util.IslamicCalendar
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.koussa.islamicazkar.R
import com.koussa.islamicazkar.data.Hijri
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Place
import com.koussa.islamicazkar.data.Prayers
import com.koussa.islamicazkar.data.Script
import com.koussa.islamicazkar.data.Settings
import com.koussa.islamicazkar.data.digits
import com.koussa.islamicazkar.i18n.more
import com.koussa.islamicazkar.notify.LockGlance
import java.util.Calendar
import java.util.TimeZone

private const val GOLD = 0xFFE6C676.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
private const val WHITE_SOFT = 0xB8FFFFFF.toInt()

/** "5:12" / "١٧:٣٢" in the place's own time zone. */
fun prayerClock(lang: Lang, millis: Long, tz: String): String {
    val c = Calendar.getInstance(TimeZone.getTimeZone(tz)).apply { timeInMillis = millis }
    val m = c.get(Calendar.MINUTE)
    return lang.digits(c.get(Calendar.HOUR_OF_DAY)) + ":" + lang.digits(m / 10) + lang.digits(m % 10)
}

/** Artwork for the prayer widgets: names in calligraphy, the next prayer highlighted. */
private object PrayerArt {
    /** Six columns (wide widgets): name in calligraphy, time, and the translated name. */
    fun columns(ctx: Context, lang: Lang, place: Place, times: LongArray, next: Int, wDp: Int, hDp: Int): Bitmap {
        val (bmp, c, d) = WidgetArt.canvas(ctx, wDp, hDp)
        val w = bmp.width.toFloat(); val h = bmp.height.toFloat()
        val script = Script.current(ctx)
        val k = WidgetArt.scriptScale(script)
        val cal = script.typeface(ctx)
        val sans = WidgetArt.font(ctx, R.font.inter, Typeface.DEFAULT)
        val names = more(Lang.AR).prayers
        val latin = more(lang).prayers
        val slot = w / 6
        val withLatin = lang != Lang.AR && h > 64 * d
        for (i in 0 until 6) {
            val col = if (lang == Lang.AR) 5 - i else i
            val cx = slot * col + slot / 2
            if (i == next) {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33D6B36A }
                c.drawRoundRect(RectF(cx - slot / 2 + 2 * d, 0f, cx + slot / 2 - 2 * d, h), 14 * d, 14 * d, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = d; p.color = 0x80F1DBA3.toInt()
                c.drawRoundRect(RectF(cx - slot / 2 + 2 * d, 0f, cx + slot / 2 - 2 * d, h), 14 * d, 14 * d, p)
            }
            val fade = if (i == Prayers.SUNRISE) 0x99 else 0xFF
            val np = WidgetArt.paint(cal, h * (if (withLatin) 0.24f else 0.30f) * k, if (i == next) GOLD else (GOLD and 0x00FFFFFF) or (fade shl 24))
            WidgetArt.fit(np, names[i], slot * 0.8f)
            c.drawText(names[i], cx, h * (if (withLatin) 0.36f else 0.44f), np)
            val tp = WidgetArt.paint(sans, (if (withLatin) 13f else 14f) * d, if (i == next) WHITE else (WHITE and 0x00FFFFFF) or ((if (i == Prayers.SUNRISE) 0x99 else 0xE6) shl 24))
            val t = prayerClock(lang, times[i], place.tz)
            WidgetArt.fit(tp, t, slot * 0.9f)
            c.drawText(t, cx, h * (if (withLatin) 0.66f else 0.80f), tp)
            if (withLatin) {
                val lp = WidgetArt.paint(sans, 9.5f * d, WHITE_SOFT)
                WidgetArt.fit(lp, latin[i], slot * 0.9f)
                c.drawText(latin[i], cx, h * 0.88f, lp)
            }
        }
        return bmp
    }

    /** Six rows (large widgets): calligraphy name at the start, time at the end. */
    fun rows(ctx: Context, lang: Lang, place: Place, times: LongArray, next: Int, wDp: Int, hDp: Int): Bitmap {
        val (bmp, c, d) = WidgetArt.canvas(ctx, wDp, hDp)
        val w = bmp.width.toFloat(); val h = bmp.height.toFloat()
        val script = Script.current(ctx)
        val k = WidgetArt.scriptScale(script)
        val cal = script.typeface(ctx)
        val sans = WidgetArt.font(ctx, R.font.inter, Typeface.DEFAULT)
        val names = more(Lang.AR).prayers
        val latin = more(lang).prayers
        val row = h / 6
        val rtl = lang == Lang.AR
        val pad = 12 * d
        for (i in 0 until 6) {
            val top = row * i
            if (i == next) {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33D6B36A }
                c.drawRoundRect(RectF(0f, top + 2 * d, w, top + row - 2 * d), 12 * d, 12 * d, p)
            } else if (i > 0 && i != next + 1) {
                val p = Paint().apply { color = 0x1FFFFFFF }
                c.drawRect(pad, top, w - pad, top + d * 0.7f, p)
            }
            val base = top + row * 0.68f
            val np = WidgetArt.paint(cal, row * 0.56f * k, if (i == next) GOLD else 0xE6E6C676.toInt(), if (rtl) Paint.Align.RIGHT else Paint.Align.LEFT)
            c.drawText(names[i], if (rtl) w - pad else pad, base, np)
            if (!rtl) {
                val lp = WidgetArt.paint(sans, 11f * d, WHITE_SOFT, Paint.Align.LEFT)
                c.drawText(latin[i], pad + np.measureText(names[i]) + 10 * d, base, lp)
            }
            val tp = WidgetArt.paint(sans, 15f * d, if (i == next) WHITE else 0xD9FFFFFF.toInt(), if (rtl) Paint.Align.LEFT else Paint.Align.RIGHT)
            tp.isFakeBoldText = i == next
            c.drawText(prayerClock(lang, times[i], place.tz), if (rtl) pad else w - pad, base, tp)
        }
        return bmp
    }

    /** A ring that fills from the previous prayer to the next, with the next prayer's name inside. */
    fun ring(ctx: Context, lang: Lang, place: Place, sizeDp: Int, now: Long): Bitmap {
        val (bmp, c, d) = WidgetArt.canvas(ctx, sizeDp, sizeDp)
        val s = bmp.width.toFloat()
        val (ni, nt) = Prayers.next(ctx, place, now)
        val (_, pt) = Prayers.previous(ctx, place, now)
        val frac = ((now - pt).toFloat() / (nt - pt).coerceAtLeast(1)).coerceIn(0f, 1f)
        val script = Script.current(ctx)
        val name = WidgetArt.paint(script.typeface(ctx), s * 0.2f * WidgetArt.scriptScale(script), GOLD)
        WidgetArt.fit(name, more(Lang.AR).prayers[ni], s * 0.62f)
        WidgetArt.ring(c, s / 2, s * 0.42f, s * 0.35f, frac, s * 0.06f, more(Lang.AR).prayers[ni], name, null, null)
        val t = WidgetArt.paint(WidgetArt.font(ctx, R.font.inter, Typeface.DEFAULT), s * 0.12f, WHITE)
        c.drawText(prayerClock(lang, nt, place.tz), s / 2, s * 0.97f, t)
        return bmp
    }
}

// ───────────────────────── Prayer times ─────────────────────────

/**
 * Prayer times for the saved place. Sizes: a ring (1×1), a strip (wide and slim), the next
 * prayer with a countdown (2×2), all six as columns (4×2) or as a full table (4×4).
 */
class PrayerTimesWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { mgr.updateAppWidget(it, views(ctx, it)) }
        PrayerTicker.schedule(ctx)
    }
    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, o: Bundle) = mgr.updateAppWidget(id, views(ctx, id))

    companion object {
        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            Widgets.ids(ctx, PrayerTimesWidget::class.java).forEach { mgr.updateAppWidget(it, views(ctx, it)) }
        }

        private fun countdown(v: RemoteViews, id: Int, target: Long, now: Long, lang: Lang) {
            val format = when (lang) { Lang.AR -> "بعد %s"; Lang.EN -> "in %s"; Lang.FR -> "dans %s" }
            v.setChronometer(id, SystemClock.elapsedRealtime() + (target - now), format, true)
            v.setChronometerCountDown(id, true)
        }

        fun views(ctx: Context, id: Int, size: Pair<Int, Int> = widgetSize(ctx, id, 320, 170), now: Long = System.currentTimeMillis()): RemoteViews {
            val lang = Settings.get(ctx).lang
            val s = more(lang)
            val (w, h) = size
            val place = Prayers.place(ctx)
            val open = openApp(ctx, 7000 + id, tab = 4)
            val script = Script.current(ctx)
            val cal = script.typeface(ctx)
            val k = WidgetArt.scriptScale(script)

            if (place == null) {
                val v = RemoteViews(ctx.packageName, R.layout.widget_prayer_small)
                v.setTextViewText(R.id.p_title, s.prayerTimes)
                v.setImageViewBitmap(R.id.p_art, WidgetArt.text(ctx, s.setLocationWidget, WidgetArt.font(ctx, R.font.inter, Typeface.DEFAULT), WHITE, w - 24, (h - 50).coerceAtLeast(30), 14f, minSp = 8f, rtl = lang == Lang.AR))
                v.setViewVisibility(R.id.p_time, View.GONE)
                v.setViewVisibility(R.id.p_count, View.GONE)
                v.setOnClickPendingIntent(R.id.p_root, open)
                Widgets.glass(ctx, v, R.id.p_root)
                return v
            }

            val times = Prayers.times(ctx, place, now)
            val (next, at) = Prayers.next(ctx, place, now)
            val nextIndex = if (at > times[Prayers.ISHA]) -1 else next  // tomorrow's Fajr: nothing to highlight today
            val v: RemoteViews
            when {
                w < 100 && h < 100 -> {
                    v = RemoteViews(ctx.packageName, R.layout.widget_prayer_ring)
                    v.setImageViewBitmap(R.id.p_art, PrayerArt.ring(ctx, lang, place, minOf(w, h), now))
                }
                h < 100 -> {
                    v = RemoteViews(ctx.packageName, R.layout.widget_prayer_strip)
                    v.setImageViewBitmap(R.id.p_art, WidgetArt.text(ctx, more(Lang.AR).prayers[next], cal, GOLD, w - 130, h - 16, 30f * k, minSp = 10f))
                    v.setTextViewText(R.id.p_time, prayerClock(lang, at, place.tz))
                    countdown(v, R.id.p_count, at, now, lang)
                }
                w < 230 -> {
                    v = RemoteViews(ctx.packageName, R.layout.widget_prayer_small)
                    v.setTextViewText(R.id.p_title, s.nextPrayer)
                    v.setImageViewBitmap(R.id.p_art, WidgetArt.text(ctx, more(Lang.AR).prayers[next], cal, GOLD, w - 24, (h - 92).coerceAtLeast(30), 40f * k, minSp = 12f))
                    v.setTextViewText(R.id.p_time, prayerClock(lang, at, place.tz) + if (lang == Lang.AR) "" else "  ·  " + s.prayers[next])
                    countdown(v, R.id.p_count, at, now, lang)
                }
                else -> {
                    v = RemoteViews(ctx.packageName, R.layout.widget_prayer)
                    val large = h >= 230
                    v.setTextViewText(R.id.p_title, place.label(lang).ifBlank { s.myLocation })
                    val hd = Hijri.of(ctx, now)
                    v.setTextViewText(R.id.p_sub, if (large) Hijri.format(lang, hd) else s.prayers[next])
                    countdown(v, R.id.p_count, at, now, lang)
                    val artH = h - (if (large) 74 else 58)
                    v.setImageViewBitmap(
                        R.id.p_art,
                        if (large) PrayerArt.rows(ctx, lang, place, times, nextIndex, w - 24, artH)
                        else PrayerArt.columns(ctx, lang, place, times, nextIndex, w - 20, artH),
                    )
                }
            }
            v.setOnClickPendingIntent(R.id.p_root, open)
            v.direction(R.id.p_root, lang)
            Widgets.glass(ctx, v, R.id.p_root)
            return v
        }
    }
}

/** Wakes the prayer widgets and the lock-screen glance right when the next prayer begins. */
object PrayerTicker {
    private const val ACTION = "com.koussa.islamicazkar.widget.PRAYER_TICK"

    fun schedule(ctx: Context) {
        val place = Prayers.place(ctx) ?: return
        val (_, at) = Prayers.next(ctx, place)
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(ctx, 7100, Intent(ctx, WidgetTickReceiver::class.java).setAction(ACTION), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.setWindow(AlarmManager.RTC, at + 1_000L, 60_000L, pi)
    }
}

// ───────────────────────── Digital clock ─────────────────────────

/** A large digital clock (kept live by the launcher) over the Hijri and Gregorian date. */
class DigitalClockWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = ids.forEach { mgr.updateAppWidget(it, views(ctx, it)) }
    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, o: Bundle) = mgr.updateAppWidget(id, views(ctx, id))

    companion object {
        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            Widgets.ids(ctx, DigitalClockWidget::class.java).forEach { mgr.updateAppWidget(it, views(ctx, it)) }
        }

        /** Weekday in calligraphy over the Hijri date, for the small clock. */
        private fun stacked(ctx: Context, lang: Lang, wDp: Int, hDp: Int, now: Calendar): Bitmap {
            val (bmp, c, d) = WidgetArt.canvas(ctx, wDp, hDp)
            val w = bmp.width.toFloat(); val h = bmp.height.toFloat()
            val script = Script.current(ctx)
            val wd = WidgetArt.paint(script.typeface(ctx), h * 0.42f * WidgetArt.scriptScale(script), GOLD)
            val weekday = Hijri.weekdayAr(now.get(Calendar.DAY_OF_WEEK) - 1)
            WidgetArt.fit(wd, weekday, w * 0.9f)
            c.drawText(weekday, w / 2, h * 0.46f, wd)
            val date = Hijri.format(lang, Hijri.of(ctx, now.timeInMillis))
            val dp = WidgetArt.paint(WidgetArt.font(ctx, R.font.inter, Typeface.DEFAULT), 12f * d, WHITE_SOFT)
            WidgetArt.fit(dp, date, w * 0.95f)
            c.drawText(date, w / 2, h * 0.86f, dp)
            return bmp
        }

        fun views(ctx: Context, id: Int, size: Pair<Int, Int> = widgetSize(ctx, id, 320, 170), now: Calendar = Calendar.getInstance()): RemoteViews {
            val lang = Settings.get(ctx).lang
            val (w, h) = size
            val slim = h < 100
            val v = RemoteViews(ctx.packageName, if (slim) R.layout.widget_digital_strip else R.layout.widget_digital)
            val artW = if (slim) (w * 0.58f).toInt() else w - 24
            val artH = if (slim) h - 16 else ((h - 24) * 0.42f).toInt()
            v.setImageViewBitmap(R.id.d_date, if (!slim && w < 230) stacked(ctx, lang, artW, artH, now) else WidgetArt.hijri(ctx, lang, artW, artH.coerceAtMost(80), now))
            v.setOnClickPendingIntent(R.id.d_root, openApp(ctx, 7200 + id))
            v.direction(R.id.d_root, lang)
            Widgets.glass(ctx, v, R.id.d_root)
            return v
        }
    }
}

// ───────────────────────── Progress ─────────────────────────

/** How much of the day, month, year and Hijri year has passed, as gold rings or bars. */
class ProgressWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = ids.forEach { mgr.updateAppWidget(it, views(ctx, it)) }
    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, o: Bundle) = mgr.updateAppWidget(id, views(ctx, id))

    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != ACTION_NEXT) return super.onReceive(ctx, intent)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        val sp = Settings.prefs(ctx)
        sp.edit().putInt("progress_$id", (sp.getInt("progress_$id", 0) + 1) % 4).apply()
        AppWidgetManager.getInstance(ctx).updateAppWidget(id, views(ctx, id))
    }

    companion object {
        private const val ACTION_NEXT = "com.koussa.islamicazkar.widget.PROGRESS_NEXT"

        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            Widgets.ids(ctx, ProgressWidget::class.java).forEach { mgr.updateAppWidget(it, views(ctx, it)) }
        }

        /** Day, month, year and Hijri year: label and fraction passed. */
        fun values(ctx: Context, lang: Lang, now: Calendar): List<Pair<String, Float>> {
            val s = more(lang)
            fun frac(field: Int, max: Int, sub: Double) = ((now.get(field) - 1 + sub) / max).toFloat()
            val dayFrac = (now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)) / 1440.0
            val ic = IslamicCalendar().apply {
                calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
                timeInMillis = now.timeInMillis + Settings.prefs(ctx).getInt(Settings.K_HIJRI_OFFSET, 0) * 86_400_000L
            }
            val hijri = ((ic.get(IslamicCalendar.DAY_OF_YEAR) - 1 + dayFrac) / ic.getActualMaximum(IslamicCalendar.DAY_OF_YEAR)).toFloat()
            return listOf(
                s.day to dayFrac.toFloat(),
                s.month to frac(Calendar.DAY_OF_MONTH, now.getActualMaximum(Calendar.DAY_OF_MONTH), dayFrac),
                s.year to frac(Calendar.DAY_OF_YEAR, now.getActualMaximum(Calendar.DAY_OF_YEAR), dayFrac),
                s.hijriYear to hijri,
            )
        }

        private fun pct(lang: Lang, f: Float) = lang.digits((f * 100).toInt()) + if (lang == Lang.AR) "٪" else "%"

        fun views(ctx: Context, id: Int, size: Pair<Int, Int> = widgetSize(ctx, id, 320, 170), now: Calendar = Calendar.getInstance()): RemoteViews {
            val lang = Settings.get(ctx).lang
            val (w, h) = size
            val items = values(ctx, lang, now)
            val v = RemoteViews(ctx.packageName, R.layout.widget_progress)
            val (bmp, c, d) = WidgetArt.canvas(ctx, w - 16, h - 16)
            val bw = bmp.width.toFloat(); val bh = bmp.height.toFloat()
            val sans = WidgetArt.font(ctx, R.font.inter, Typeface.DEFAULT)
            val rtl = lang == Lang.AR
            val labelTf = if (rtl) WidgetArt.font(ctx, R.font.amiri_bold) else sans
            when {
                h < 100 -> {
                    // Four slim bars side by side.
                    val slot = bw / 4
                    items.forEachIndexed { i, (label, f) ->
                        val col = if (rtl) 3 - i else i
                        val x0 = slot * col + 8 * d; val x1 = slot * (col + 1) - 8 * d
                        val lp = WidgetArt.paint(labelTf, 11f * d, WHITE_SOFT, Paint.Align.CENTER)
                        WidgetArt.fit(lp, label, x1 - x0)
                        c.drawText(label, (x0 + x1) / 2, bh * 0.32f, lp)
                        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x2EFFFFFF }
                        val r = 3 * d
                        c.drawRoundRect(RectF(x0, bh * 0.46f, x1, bh * 0.46f + 2 * r), r, r, track)
                        track.color = GOLD
                        val fx = x0 + (x1 - x0) * f
                        if (rtl) c.drawRoundRect(RectF(x1 - (fx - x0), bh * 0.46f, x1, bh * 0.46f + 2 * r), r, r, track)
                        else c.drawRoundRect(RectF(x0, bh * 0.46f, fx, bh * 0.46f + 2 * r), r, r, track)
                        val vp = WidgetArt.paint(sans, 12f * d, WHITE, Paint.Align.CENTER)
                        c.drawText(pct(lang, f), (x0 + x1) / 2, bh * 0.86f, vp)
                    }
                }
                w < 230 -> {
                    // One ring; tap to switch between day, month, year and Hijri year.
                    val which = Settings.prefs(ctx).getInt("progress_$id", 0)
                    val (label, f) = items[which]
                    val r = minOf(bw, bh) * 0.33f
                    WidgetArt.ring(c, bw / 2, bh * 0.44f, r, f, r * 0.17f, pct(lang, f), WidgetArt.paint(sans, r * 0.42f, WHITE), label, WidgetArt.paint(labelTf, 12f * d, GOLD))
                    v.setOnClickPendingIntent(R.id.g_root, selfIntent(ctx, ProgressWidget::class.java, ACTION_NEXT, id, 1))
                }
                else -> {
                    val slot = bw / 4
                    val r = minOf(slot * 0.36f, bh * 0.3f)
                    items.forEachIndexed { i, (label, f) ->
                        val col = if (rtl) 3 - i else i
                        WidgetArt.ring(c, slot * col + slot / 2, bh * 0.42f, r, f, r * 0.2f, pct(lang, f), WidgetArt.paint(sans, r * 0.46f, WHITE), label, WidgetArt.paint(labelTf, 11.5f * d, GOLD).also { WidgetArt.fit(it, label, slot * 0.95f) })
                    }
                }
            }
            v.setImageViewBitmap(R.id.g_art, bmp)
            if (!(w < 230 && h >= 100)) v.setOnClickPendingIntent(R.id.g_root, openApp(ctx, 7300 + id))
            Widgets.glass(ctx, v, R.id.g_root)
            return v
        }
    }
}

/** Refreshes the widgets in this file and the lock-screen glance. */
object MoreWidgets {
    fun refreshAll(ctx: Context) {
        PrayerTimesWidget.refresh(ctx)
        DigitalClockWidget.refresh(ctx)
        ProgressWidget.refresh(ctx)
        PrayerTicker.schedule(ctx)
        LockGlance.update(ctx)
    }
}
