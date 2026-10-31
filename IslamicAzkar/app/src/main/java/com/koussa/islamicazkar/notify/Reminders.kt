package com.koussa.islamicazkar.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.koussa.islamicazkar.MainActivity
import com.koussa.islamicazkar.R
import com.koussa.islamicazkar.data.AzkarRepository
import com.koussa.islamicazkar.data.Settings
import com.koussa.islamicazkar.i18n.strings
import java.util.Calendar
import kotlin.random.Random

/** Schedules the morning, evening and general dhikr reminders with AlarmManager. */
object Reminders {
    private const val CH_DAILY = "daily_azkar"
    private const val CH_GENERAL = "general_dhikr"
    const val EXTRA_KIND = "kind"
    const val KIND_MORNING = 1
    const val KIND_EVENING = 2
    const val KIND_GENERAL = 3

    fun reschedule(ctx: Context) {
        val st = Settings.get(ctx)
        ensureChannels(ctx)
        setOrCancel(ctx, KIND_MORNING, if (st.morningOn) nextDaily(st.morningAt) else null)
        setOrCancel(ctx, KIND_EVENING, if (st.eveningOn) nextDaily(st.eveningAt) else null)
        setOrCancel(ctx, KIND_GENERAL, if (st.generalOn) nextGeneral(st.generalEveryHours, st.generalFrom, st.generalTo) else null)
    }

    private fun nextDaily(minutes: Int, now: Calendar = Calendar.getInstance()): Long {
        val c = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60); set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (!c.after(now)) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    /** Next slot on the grid from, from+every, … that falls within [from, to]. */
    private fun nextGeneral(everyHours: Int, from: Int, to: Int, now: Calendar = Calendar.getInstance()): Long {
        val end = if (to <= from) from else to
        val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        var slot = from
        while (slot <= end) {
            if (slot > nowMin) return nextDaily(slot, now) // still today
            slot += everyHours * 60
        }
        return nextDaily(from, now) // tomorrow's first slot
    }

    private fun pending(ctx: Context, kind: Int): PendingIntent = PendingIntent.getBroadcast(
        ctx, kind, Intent(ctx, ReminderReceiver::class.java).putExtra(EXTRA_KIND, kind),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun setOrCancel(ctx: Context, kind: Int, at: Long?) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, kind)
        if (at == null) { am.cancel(pi); return }
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    private fun ensureChannels(ctx: Context) {
        val s = strings(Settings.get(ctx).lang)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_DAILY, s.channelDaily, NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(CH_GENERAL, s.channelGeneral, NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun show(ctx: Context, kind: Int) {
        val st = Settings.get(ctx)
        val s = strings(st.lang)
        ensureChannels(ctx)
        val open = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val b = NotificationCompat.Builder(ctx, if (kind == KIND_GENERAL) CH_GENERAL else CH_DAILY)
            .setSmallIcon(R.drawable.ic_star)
            .setColor(0xFF0C8A69.toInt())
            .setAutoCancel(true)
        when (kind) {
            KIND_MORNING -> {
                open.putExtra(MainActivity.EXTRA_OPEN, "morning")
                b.setContentTitle(s.notifMorningTitle).setContentText(s.notifMorningBody)
            }
            KIND_EVENING -> {
                open.putExtra(MainActivity.EXTRA_OPEN, "evening")
                b.setContentTitle(s.notifEveningTitle).setContentText(s.notifEveningBody)
            }
            else -> {
                val data = AzkarRepository.get(ctx)
                val z = data.shortAzkar[Random.nextInt(data.shortAzkar.size)]
                val chapter = data.chapterOfZikr[z.id]
                open.putExtra(MainActivity.EXTRA_OPEN, when (chapter) { -1 -> "morning"; -2 -> "evening"; else -> "c$chapter" })
                open.putExtra(MainActivity.EXTRA_FOCUS, z.id)
                val meaning = z.meaning(st.lang)
                b.setContentTitle(s.notifGeneralTitle)
                    .setContentText(z.ar)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(if (meaning.isBlank()) z.ar else "${z.ar}\n\n$meaning"))
            }
        }
        b.setContentIntent(PendingIntent.getActivity(ctx, 100 + kind, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        runCatching { NotificationManagerCompat.from(ctx).notify(kind, b.build()) }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.show(context, intent.getIntExtra(Reminders.EXTRA_KIND, Reminders.KIND_GENERAL))
        Reminders.reschedule(context)
    }
}

/** Alarms are cleared on reboot or clock changes; put them back. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.reschedule(context)
        com.koussa.islamicazkar.widget.WidgetTicker.schedule(context)
        com.koussa.islamicazkar.widget.AzkarListWidget.refreshAll(context)
        com.koussa.islamicazkar.widget.IslamicWidgets.refreshAll(context)
        com.koussa.islamicazkar.widget.MoreWidgets.refreshAll(context)
    }
}
