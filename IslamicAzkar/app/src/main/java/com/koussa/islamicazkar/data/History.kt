package com.koussa.islamicazkar.data

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * Daily record of completed azkar, kept on the device: for each day ("2026-11-06"), the readings
 * completed in full that day ("morning", "evening", "c28" — any chapter of any category).
 * The same format as the iPhone / Mac app.
 */
object AzkarHistory {
    private const val KEY = "azkar_history"

    /** Bumped on every change so the Record screen recomposes. */
    val version = mutableIntStateOf(0)

    fun dayKey(c: Calendar): String =
        "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))

    fun dayKey(millis: Long = System.currentTimeMillis()): String =
        dayKey(Calendar.getInstance().apply { timeInMillis = millis })

    /** Day key → readings completed that day. */
    fun all(ctx: Context): Map<String, List<String>> {
        val text = Settings.prefs(ctx).getString(KEY, null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(text)
            o.keys().asSequence().associateWith { k ->
                val r = o.getJSONObject(k).optJSONArray("readings") ?: JSONArray()
                List(r.length()) { r.getString(it) }
            }
        }.getOrDefault(emptyMap())
    }

    /** A whole reading (morning, evening, a chapter) was completed now. */
    fun markReading(ctx: Context, key: String) {
        val sp = Settings.prefs(ctx)
        val o = runCatching { JSONObject(sp.getString(KEY, null) ?: "{}") }.getOrDefault(JSONObject())
        val today = dayKey()
        val day = o.optJSONObject(today) ?: JSONObject().put("zikr", JSONArray()).put("readings", JSONArray())
        val readings = day.optJSONArray("readings") ?: JSONArray()
        if ((0 until readings.length()).none { readings.getString(it) == key }) readings.put(key)
        day.put("readings", readings)
        o.put(today, day)
        sp.edit().putString(KEY, o.toString()).apply()
        version.intValue++
        AzkarSync.syncInBackground(ctx)   // show it on the other devices
    }

    /** Adds readings completed on the other devices ([remote]: day → array of readings). */
    fun merge(ctx: Context, remote: JSONObject) {
        val sp = Settings.prefs(ctx)
        val o = runCatching { JSONObject(sp.getString(KEY, null) ?: "{}") }.getOrDefault(JSONObject())
        var changed = false
        remote.keys().forEach { k ->
            val add = remote.optJSONArray(k) ?: return@forEach
            val day = o.optJSONObject(k) ?: JSONObject().put("zikr", JSONArray()).put("readings", JSONArray())
            val readings = day.optJSONArray("readings") ?: JSONArray()
            val have = (0 until readings.length()).map { readings.getString(it) }.toMutableSet()
            for (i in 0 until add.length()) {
                val r = add.getString(i)
                if (have.add(r)) { readings.put(r); changed = true }
            }
            day.put("readings", readings)
            o.put(k, day)
        }
        if (changed) {
            sp.edit().putString(KEY, o.toString()).apply()
            android.os.Handler(android.os.Looper.getMainLooper()).post { version.intValue++ }
        }
    }

    /**
     * Consecutive days on which [key] (e.g. "morning", "c28") was completed, ending today (or
     * yesterday, if it is not done yet today). Counts every device's completions (the record syncs).
     */
    fun streak(ctx: Context, key: String): Int {
        val days = all(ctx)
        val c = Calendar.getInstance()
        if (days[dayKey(c)]?.contains(key) != true) c.add(Calendar.DAY_OF_MONTH, -1)
        var n = 0
        while (days[dayKey(c)]?.contains(key) == true) {
            n++
            c.add(Calendar.DAY_OF_MONTH, -1)
        }
        return n
    }

    /** Consecutive days with at least one completed reading, ending today (or yesterday). */
    fun currentStreak(days: Map<String, List<String>>): Int {
        val c = Calendar.getInstance()
        if (days[dayKey(c)].isNullOrEmpty()) c.add(Calendar.DAY_OF_MONTH, -1)
        var n = 0
        while (!days[dayKey(c)].isNullOrEmpty()) {
            n++
            c.add(Calendar.DAY_OF_MONTH, -1)
        }
        return n
    }
}
