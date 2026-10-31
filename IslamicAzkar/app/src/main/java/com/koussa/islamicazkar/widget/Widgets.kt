package com.koussa.islamicazkar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.koussa.islamicazkar.MainActivity
import com.koussa.islamicazkar.R
import com.koussa.islamicazkar.data.AzkarRepository
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Settings
import com.koussa.islamicazkar.data.TasbihPhrases
import com.koussa.islamicazkar.i18n.num
import com.koussa.islamicazkar.i18n.strings

object Widgets {
    fun ids(ctx: Context, cls: Class<*>): IntArray =
        AppWidgetManager.getInstance(ctx).getAppWidgetIds(ComponentName(ctx, cls))

    fun openApp(ctx: Context, requestCode: Int, open: String?, focus: Int? = null): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (open != null) i.putExtra(MainActivity.EXTRA_OPEN, open)
        if (focus != null) i.putExtra(MainActivity.EXTRA_FOCUS, focus)
        return PendingIntent.getActivity(ctx, requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun self(ctx: Context, cls: Class<*>, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, requestCode, Intent(ctx, cls).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Paints [root] with the glass of the user's chosen palette. */
    fun glass(ctx: Context, v: RemoteViews, root: Int) =
        v.setInt(root, "setBackgroundResource", com.koussa.islamicazkar.data.Palette.current(ctx).glass)

    /** Re-renders every widget, e.g. after the language, palette or calligraphy changes. */
    fun refreshEverything(ctx: Context) {
        DhikrWidget.refresh(ctx)
        QuickWidget.refresh(ctx)
        refreshTasbih(ctx)
        AzkarListWidget.refreshAll(ctx)
        IslamicWidgets.refreshAll(ctx)
        MoreWidgets.refreshAll(ctx)
    }

    fun refreshTasbih(ctx: Context) {
        TasbihWidget.refresh(ctx)
        CounterStatsWidget.refresh(ctx)
    }
}

/** "Dhikr of the day": a rotating short dhikr with its translation; tap to open, ↻ for the next one. */
class DhikrWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        // Advance on the system's periodic update so the widget feels alive through the day.
        advance(ctx)
        render(ctx, mgr, ids)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == ACTION_NEXT) {
            advance(ctx)
            refresh(ctx)
        } else super.onReceive(ctx, intent)
    }

    companion object {
        private const val ACTION_NEXT = "com.koussa.islamicazkar.widget.NEXT_DHIKR"
        private const val K_INDEX = "widget_dhikr_index"

        private fun advance(ctx: Context) {
            val sp = Settings.prefs(ctx)
            sp.edit().putInt(K_INDEX, sp.getInt(K_INDEX, -1) + 1).apply()
        }

        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            render(ctx, mgr, Widgets.ids(ctx, DhikrWidget::class.java))
        }

        private fun render(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
            if (ids.isEmpty()) return
            val data = AzkarRepository.get(ctx)
            val lang = Settings.get(ctx).lang
            val pool = data.shortAzkar
            val idx = Math.floorMod(Settings.prefs(ctx).getInt(K_INDEX, 0), pool.size)
            val z = pool[idx]
            val chapter = data.chapterOfZikr[z.id]
            val open = when (chapter) { -1 -> "morning"; -2 -> "evening"; null -> null; else -> "c$chapter" }
            val s = strings(lang)
            for (id in ids) {
                val v = RemoteViews(ctx.packageName, R.layout.widget_dhikr)
                Widgets.glass(ctx, v, R.id.w_root)
                v.setInt(R.id.w_root, "setLayoutDirection", if (lang == Lang.AR) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR)
                v.setTextViewText(R.id.w_title, s.widgetDhikr)
                v.setTextViewText(R.id.w_arabic, z.ar)
                val meaning = z.meaning(lang)
                if (meaning.isBlank()) v.setViewVisibility(R.id.w_meaning, View.GONE)
                else { v.setViewVisibility(R.id.w_meaning, View.VISIBLE); v.setTextViewText(R.id.w_meaning, meaning) }
                v.setOnClickPendingIntent(R.id.w_root, Widgets.openApp(ctx, 200, open, z.id))
                v.setOnClickPendingIntent(R.id.w_next, Widgets.self(ctx, DhikrWidget::class.java, ACTION_NEXT, 201))
                mgr.updateAppWidget(id, v)
            }
        }
    }
}

/** One-tap shortcuts into morning, evening and sleep azkar. */
class QuickWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = render(ctx, mgr, ids)

    companion object {
        fun refresh(ctx: Context) {
            render(ctx, AppWidgetManager.getInstance(ctx), Widgets.ids(ctx, QuickWidget::class.java))
        }

        private fun render(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
            if (ids.isEmpty()) return
            val s = strings(Settings.get(ctx).lang)
            for (id in ids) {
                val v = RemoteViews(ctx.packageName, R.layout.widget_quick)
                Widgets.glass(ctx, v, R.id.q_root)
                v.setTextViewText(R.id.q_morning_label, s.morningAzkar)
                v.setTextViewText(R.id.q_evening_label, s.eveningAzkar)
                v.setTextViewText(R.id.q_sleep_label, s.sleepAzkar)
                v.setOnClickPendingIntent(R.id.q_morning, Widgets.openApp(ctx, 210, "morning"))
                v.setOnClickPendingIntent(R.id.q_evening, Widgets.openApp(ctx, 211, "evening"))
                v.setOnClickPendingIntent(R.id.q_sleep, Widgets.openApp(ctx, 212, "c28"))
                mgr.updateAppWidget(id, v)
            }
        }
    }
}

/** A tasbih counter right on the home screen, shared with the in-app counter. */
class TasbihWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) = render(ctx, mgr, ids)

    override fun onReceive(ctx: Context, intent: Intent) {
        val sp = Settings.prefs(ctx)
        when (intent.action) {
            ACTION_TAP -> sp.edit()
                .putInt(Settings.K_TASBIH_COUNT, sp.getInt(Settings.K_TASBIH_COUNT, 0) + 1)
                .putInt(Settings.K_TASBIH_TOTAL, sp.getInt(Settings.K_TASBIH_TOTAL, 0) + 1)
                .commit().also { com.koussa.islamicazkar.data.TasbihHistory.add(ctx) }
            ACTION_RESET -> sp.edit().putInt(Settings.K_TASBIH_COUNT, 0).commit()
            else -> return super.onReceive(ctx, intent)
        }
        Settings.get(ctx).reloadTasbih()
        refresh(ctx)
    }

    companion object {
        private const val ACTION_TAP = "com.koussa.islamicazkar.widget.TASBIH_TAP"
        private const val ACTION_RESET = "com.koussa.islamicazkar.widget.TASBIH_RESET"

        fun refresh(ctx: Context) {
            render(ctx, AppWidgetManager.getInstance(ctx), Widgets.ids(ctx, TasbihWidget::class.java))
        }

        private fun render(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
            if (ids.isEmpty()) return
            val sp = Settings.prefs(ctx)
            val lang = Lang.of(sp.getString("lang", null))
            val count = sp.getInt(Settings.K_TASBIH_COUNT, 0)
            val target = sp.getInt(Settings.K_TASBIH_TARGET, 33)
            val phrase = sp.getInt(Settings.K_TASBIH_PHRASE, 0).coerceIn(0, TasbihPhrases.ar.lastIndex)
            val inRound = if (target > 0) count % target else count
            for (id in ids) {
                val v = RemoteViews(ctx.packageName, R.layout.widget_tasbih)
                Widgets.glass(ctx, v, R.id.t_root)
                v.setTextViewText(R.id.t_phrase, TasbihPhrases.ar[phrase])
                v.setTextViewText(R.id.t_count, lang.num(inRound))
                v.setTextViewText(R.id.t_target, if (target > 0) "/ ${lang.num(target)}" else "∞")
                v.setProgressBar(R.id.t_progress, if (target > 0) target else 1, if (target > 0) inRound else 0, false)
                v.setOnClickPendingIntent(R.id.t_root, Widgets.self(ctx, TasbihWidget::class.java, ACTION_TAP, 220))
                v.setOnClickPendingIntent(R.id.t_reset, Widgets.self(ctx, TasbihWidget::class.java, ACTION_RESET, 221))
                mgr.updateAppWidget(id, v)
            }
        }
    }
}
