package com.koussa.islamicazkar.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.koussa.islamicazkar.MainActivity
import com.koussa.islamicazkar.R
import com.koussa.islamicazkar.data.Hijri
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Prayers
import com.koussa.islamicazkar.data.Settings
import com.koussa.islamicazkar.i18n.more
import com.koussa.islamicazkar.widget.prayerClock

/**
 * A quiet, ongoing notification for the lock screen: the next prayer with a live countdown
 * and today's Hijri date. It is the phone-wide equivalent of a lock-screen widget.
 */
object LockGlance {
    private const val CHANNEL = "lock_glance"
    private const val ID = 4242

    fun update(ctx: Context) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!Settings.prefs(ctx).getBoolean(Settings.K_LOCK_GLANCE, false)) { nm.cancel(ID); return }
        val lang = Settings.get(ctx).lang
        val s = more(lang)
        ctx.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CHANNEL, s.lockChannel, NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            },
        )
        val now = System.currentTimeMillis()
        val hijri = Hijri.format(lang, Hijri.of(ctx, now))
        val open = PendingIntent.getActivity(
            ctx, 4242, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val b = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_star)
            .setColor(0xFF0C8A69.toInt())
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(open)

        val place = Prayers.place(ctx)
        if (place == null) {
            b.setContentTitle(hijri).setContentText(s.setLocationWidget)
        } else {
            val (next, at) = Prayers.next(ctx, place, now)
            val name = if (lang == Lang.AR) s.prayers[next] else "${s.prayers[next]} · ${more(Lang.AR).prayers[next]}"
            b.setContentTitle("$name  ${prayerClock(lang, at, place.tz)}")
                .setContentText("$hijri · ${place.label(lang).ifBlank { s.myLocation }}")
                .setWhen(at).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
        }
        runCatching { nm.notify(ID, b.build()) }
    }
}
