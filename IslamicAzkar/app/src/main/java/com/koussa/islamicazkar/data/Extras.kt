package com.koussa.islamicazkar.data

import android.content.Context
import org.json.JSONArray

/** A Quranic verse (text from the Mushaf app's Uthmani data) with its reference and meanings. */
data class Verse(val ar: String, val en: String, val fr: String, val refAr: String, val ref: String) {
    fun meaning(lang: Lang) = when (lang) { Lang.AR -> ""; Lang.EN -> en; Lang.FR -> fr }
    fun reference(lang: Lang) = if (lang == Lang.AR) refAr else ref
}

object Verses {
    @Volatile private var cache: List<Verse>? = null

    fun all(ctx: Context): List<Verse> = cache ?: synchronized(this) {
        cache ?: run {
            val a = JSONArray(ctx.applicationContext.assets.open("verses.json").bufferedReader().use { it.readText() })
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                Verse(o.getString("ar"), o.getString("en"), o.getString("fr"), o.getString("refAr"), o.getString("ref"))
            }.also { cache = it }
        }
    }
}

/** Hadith texts from the book's chapters on the virtues of dhikr, salawat, salam and repentance. */
object Hadiths {
    private val CHAPTERS = listOf(130, 107, 129, 108, 50)

    fun all(ctx: Context): List<Zikr> {
        val data = AzkarRepository.get(ctx)
        return CHAPTERS.flatMap { data.chapterById[it]?.items.orEmpty() }.filter { it.ar.length in 30..360 }
    }
}

/** Tasbih counts per day, for the weekly statistics widget. */
object TasbihHistory {
    private fun today(): Long {
        val now = System.currentTimeMillis()
        return (now + java.util.TimeZone.getDefault().getOffset(now)) / 86_400_000L
    }

    private fun key(day: Long) = "tasbih_day_$day"

    fun add(ctx: Context, n: Int = 1) {
        val sp = Settings.prefs(ctx)
        val k = key(today())
        sp.edit().putInt(k, sp.getInt(k, 0) + n).apply()
    }

    /** The last seven days, oldest first, ending today. */
    fun lastWeek(ctx: Context): List<Pair<Long, Int>> {
        val sp = Settings.prefs(ctx)
        val t = today()
        return (6 downTo 0).map { d -> (t - d) to sp.getInt(key(t - d), 0) }
    }
}
