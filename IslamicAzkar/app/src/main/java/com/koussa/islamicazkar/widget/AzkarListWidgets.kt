package com.koussa.islamicazkar.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.koussa.islamicazkar.MainActivity
import com.koussa.islamicazkar.R
import com.koussa.islamicazkar.data.AzkarRepository
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Settings
import com.koussa.islamicazkar.data.Zikr
import com.koussa.islamicazkar.i18n.num
import com.koussa.islamicazkar.i18n.strings
import java.util.Calendar

/** Which azkar a widget cycles through. */
enum class WidgetSet { MORNING, DAY, NIGHT, TIMELINE }

/** A time-of-day period, used by the timeline widget. */
enum class Period { MORNING, DAY, EVENING, NIGHT }

object AzkarSets {
    /** Chapters whose azkar are said through the day (after prayer, home, food, gatherings, distress…). */
    private val DAY_CHAPTERS = listOf(25, 10, 11, 69, 70, 98, 85, 87, 106, 34, 35, 43, 82, 129)
    /** Before sleep and waking in the night. */
    private val SLEEP_CHAPTERS = listOf(28, 29, 30)

    fun period(now: Calendar = Calendar.getInstance()): Period = when (now.get(Calendar.HOUR_OF_DAY)) {
        in 4..10 -> Period.MORNING
        in 11..14 -> Period.DAY
        in 15..19 -> Period.EVENING
        else -> Period.NIGHT
    }

    /** Minute of the day at which [p] begins, so a timeline widget starts at the first dhikr. */
    fun periodStart(p: Period): Int = when (p) {
        Period.MORNING -> 4 * 60
        Period.DAY -> 11 * 60
        Period.EVENING -> 15 * 60
        Period.NIGHT -> 20 * 60
    }

    fun items(ctx: Context, set: WidgetSet, period: Period = period()): List<Zikr> {
        val data = AzkarRepository.get(ctx)
        fun chapters(ids: List<Int>, maxLen: Int) = ids.flatMap { data.chapterById[it]?.items.orEmpty() }.filter { it.ar.length <= maxLen }
        val sleep = chapters(SLEEP_CHAPTERS, 900)
        return when (set) {
            WidgetSet.MORNING -> data.morning
            WidgetSet.DAY -> chapters(DAY_CHAPTERS, 320)
            WidgetSet.NIGHT -> data.evening + sleep
            WidgetSet.TIMELINE -> when (period) {
                Period.MORNING -> data.morning
                Period.DAY -> chapters(DAY_CHAPTERS, 320)
                Period.EVENING -> data.evening
                Period.NIGHT -> sleep
            }
        }
    }
}

/**
 * A widget that shows one dhikr at a time from a set and moves to the next one every
 * 15 minutes (a timeline), with ‹ › buttons to step through by hand.
 */
abstract class AzkarListWidget(private val set: WidgetSet) : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        WidgetTicker.schedule(ctx)
        ids.forEach { mgr.updateAppWidget(it, views(ctx, set, it)) }
    }

    override fun onEnabled(ctx: Context) = WidgetTicker.schedule(ctx)

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, options: android.os.Bundle) {
        mgr.updateAppWidget(id, views(ctx, set, id))
    }

    override fun onDeleted(ctx: Context, ids: IntArray) {
        val e = Settings.prefs(ctx).edit()
        ids.forEach { e.remove(offsetKey(it)) }
        e.apply()
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val step = when (intent.action) {
            ACTION_NEXT -> 1
            ACTION_PREV -> -1
            else -> return super.onReceive(ctx, intent)
        }
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val sp = Settings.prefs(ctx)
        sp.edit().putInt(offsetKey(id), sp.getInt(offsetKey(id), 0) + step).apply()
        AppWidgetManager.getInstance(ctx).updateAppWidget(id, views(ctx, set, id))
    }

    companion object {
        const val ACTION_NEXT = "com.koussa.islamicazkar.widget.AZKAR_NEXT"
        const val ACTION_PREV = "com.koussa.islamicazkar.widget.AZKAR_PREV"
        private const val SLOT_MINUTES = 15

        private fun offsetKey(id: Int) = "azkar_widget_offset_$id"

        private val providers: List<Pair<Class<out AzkarListWidget>, WidgetSet>> = listOf(
            MorningAzkarWidget::class.java to WidgetSet.MORNING,
            DayAzkarWidget::class.java to WidgetSet.DAY,
            NightAzkarWidget::class.java to WidgetSet.NIGHT,
            TimelineAzkarWidget::class.java to WidgetSet.TIMELINE,
        )

        fun hasAny(ctx: Context): Boolean = providers.any { (cls, _) -> Widgets.ids(ctx, cls).isNotEmpty() }

        /** Re-renders every azkar widget (called by the ticker and when the language changes). */
        fun refreshAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            providers.forEach { (cls, set) -> Widgets.ids(ctx, cls).forEach { mgr.updateAppWidget(it, views(ctx, set, it)) } }
        }

        private fun providerClass(set: WidgetSet) = providers.first { it.second == set }.first

        /** Builds the widget; on Android 12+ it switches between a compact and a full layout by size. */
        fun views(ctx: Context, set: WidgetSet, id: Int): RemoteViews {
            val full = build(ctx, set, id, compact = false)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return full
            val compact = build(ctx, set, id, compact = true)
            return RemoteViews(mapOf(SizeF(100f, 100f) to compact, SizeF(240f, 100f) to full))
        }

        fun build(ctx: Context, set: WidgetSet, id: Int, compact: Boolean, now: Calendar = Calendar.getInstance()): RemoteViews {
            val st = Settings.get(ctx)
            val lang = st.lang
            val s = strings(lang)
            val period = AzkarSets.period(now)
            val items = AzkarSets.items(ctx, set, period)
            val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val start = if (set == WidgetSet.TIMELINE) AzkarSets.periodStart(period) else AzkarSets.periodStart(Period.MORNING)
            val elapsed = Math.floorMod(minute - start, 24 * 60)
            val index = Math.floorMod(elapsed / SLOT_MINUTES + Settings.prefs(ctx).getInt(offsetKey(id), 0), items.size)
            val z = items[index]

            val (title, icon) = when (set) {
                WidgetSet.MORNING -> s.morningAzkar to R.drawable.ic_sun
                WidgetSet.DAY -> s.dayAzkar to R.drawable.ic_day
                WidgetSet.NIGHT -> s.nightAzkar to R.drawable.ic_moon
                WidgetSet.TIMELINE -> when (period) {
                    Period.MORNING -> s.morningAzkar to R.drawable.ic_sun
                    Period.DAY -> s.dayAzkar to R.drawable.ic_day
                    Period.EVENING -> s.eveningAzkar to R.drawable.ic_sunset
                    Period.NIGHT -> s.sleepAzkar to R.drawable.ic_moon
                }
            }

            val v = RemoteViews(ctx.packageName, if (compact) R.layout.widget_azkar_small else R.layout.widget_azkar)
            // Follow the app language, not the phone's, for right-to-left layout.
            Widgets.glass(ctx, v, R.id.a_root)
            v.setInt(R.id.a_root, "setLayoutDirection", if (lang == Lang.AR) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR)
            v.setImageViewResource(R.id.a_icon, icon)
            v.setTextViewText(R.id.a_title, title)
            v.setTextViewText(R.id.a_progress, "${lang.num(index + 1)} ${s.ofTarget(lang.num(items.size))}")
            v.setTextViewText(R.id.a_arabic, z.ar)
            if (!compact) {
                val meaning = z.meaning(lang)
                v.setViewVisibility(R.id.a_meaning, if (meaning.isBlank()) View.GONE else View.VISIBLE)
                v.setTextViewText(R.id.a_meaning, meaning)
                v.setTextViewText(R.id.a_repeat, if (z.repeat > 1) "× ${lang.num(z.repeat)}" else "")
                v.setViewVisibility(R.id.a_repeat, if (z.repeat > 1) View.VISIBLE else View.GONE)
                // "Next" points the way the text reads.
                val rtl = lang == Lang.AR
                v.setImageViewResource(R.id.a_next, if (rtl) R.drawable.ic_chevron_left else R.drawable.ic_chevron_right)
                v.setImageViewResource(R.id.a_prev, if (rtl) R.drawable.ic_chevron_right else R.drawable.ic_chevron_left)
                val cls = providerClass(set)
                v.setOnClickPendingIntent(R.id.a_next, step(ctx, cls, ACTION_NEXT, id, 1))
                v.setOnClickPendingIntent(R.id.a_prev, step(ctx, cls, ACTION_PREV, id, 2))
            }
            v.setOnClickPendingIntent(R.id.a_root, open(ctx, z, id))
            return v
        }

        private fun step(ctx: Context, cls: Class<*>, action: String, id: Int, k: Int): PendingIntent =
            PendingIntent.getBroadcast(
                ctx, id * 10 + k,
                Intent(ctx, cls).setAction(action).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun open(ctx: Context, z: Zikr, id: Int): PendingIntent {
            val chapter = AzkarRepository.get(ctx).chapterOfZikr[z.id]
            val key = when (chapter) { -1 -> "morning"; -2 -> "evening"; null -> null; else -> "c$chapter" }
            val i = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (key != null) i.putExtra(MainActivity.EXTRA_OPEN, key)
            i.putExtra(MainActivity.EXTRA_FOCUS, z.id)
            return PendingIntent.getActivity(ctx, 5000 + id, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}

class MorningAzkarWidget : AzkarListWidget(WidgetSet.MORNING)
class DayAzkarWidget : AzkarListWidget(WidgetSet.DAY)
class NightAzkarWidget : AzkarListWidget(WidgetSet.NIGHT)
class TimelineAzkarWidget : AzkarListWidget(WidgetSet.TIMELINE)

/** Wakes the azkar widgets every 15 minutes so each one moves on to the next dhikr. */
object WidgetTicker {
    private fun pending(ctx: Context) = PendingIntent.getBroadcast(
        ctx, 7100, Intent(ctx, WidgetTickReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        if (!AzkarListWidget.hasAny(ctx)) { am.cancel(pending(ctx)); return }
        val c = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, 15 - get(Calendar.MINUTE) % 15)
        }
        // Inexact: lets the system batch wake-ups, so the widgets cost almost no battery.
        am.setInexactRepeating(AlarmManager.RTC, c.timeInMillis, AlarmManager.INTERVAL_FIFTEEN_MINUTES, pending(ctx))
    }
}

class WidgetTickReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        AzkarListWidget.refreshAll(ctx)
        DhikrWidget.refresh(ctx)
        AzkarCardsWidget.refresh(ctx)
        ScriptureWidget.refresh(ctx)
        HijriDateWidget.refresh(ctx)
        CounterStatsWidget.refresh(ctx) // a new day starts a new bar
        MoreWidgets.refreshAll(ctx)     // prayer times, clock date, progress, lock screen
    }
}
