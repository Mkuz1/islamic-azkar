package com.koussa.islamicazkar.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class Lang(val code: String) {
    AR("ar"), EN("en"), FR("fr");

    val isRtl: Boolean get() = this == AR

    companion object {
        fun of(code: String?): Lang = entries.firstOrNull { it.code == code } ?: AR
    }
}

/** A single dhikr / supplication. [repeat] is how many times it is said. */
data class Zikr(
    val id: Int,
    val ar: String,
    val translit: String,
    val en: String,
    val fr: String,
    val repeat: Int,
    /** Hadith reference (book and number), present on the duas added from the hadith collections. */
    val ref: Localized? = null,
) {
    fun meaning(lang: Lang): String = when (lang) {
        Lang.FR -> fr
        Lang.EN -> en
        Lang.AR -> ""
    }
}

/** A text in the three languages. */
data class Localized(val ar: String, val en: String, val fr: String) {
    operator fun get(lang: Lang): String = when (lang) {
        Lang.AR -> ar
        Lang.EN -> en
        Lang.FR -> fr
    }

    companion object {
        fun of(o: JSONObject?): Localized? = o?.let { Localized(it.optString("ar"), it.optString("en"), it.optString("fr")) }
    }
}

/** One step of a guide (e.g. ʿUmrah), with the duas it links to and its hadith sources. */
data class GuideStep(val icon: String, val title: Localized, val text: Localized, val zikr: List<Int>, val ref: Localized)

data class Guide(val key: String, val title: Localized, val intro: Localized, val note: Localized, val chapter: Int, val steps: List<GuideStep>)

data class Chapter(
    val id: Int,
    val ar: String,
    val en: String,
    val fr: String,
    val items: List<Zikr>,
) {
    fun title(lang: Lang): String = when (lang) {
        Lang.AR -> ar
        Lang.EN -> en
        Lang.FR -> fr
    }
}

data class Category(val key: String, val chapterIds: List<Int>)

/** Identifies something the reader can open: a book chapter or the curated morning / evening lists. */
sealed interface Reading {
    data class OfChapter(val id: Int) : Reading
    data object Morning : Reading
    data object Evening : Reading

    fun key(): String = when (this) {
        is OfChapter -> "c$id"
        Morning -> "morning"
        Evening -> "evening"
    }

    companion object {
        fun fromKey(key: String?): Reading? = when {
            key == null -> null
            key == "morning" -> Morning
            key == "evening" -> Evening
            key.startsWith("c") -> key.drop(1).toIntOrNull()?.let { OfChapter(it) }
            else -> null
        }
    }
}

/** Hisn Al Muslim content, parsed once from assets/azkar.json. */
object AzkarRepository {
    @Volatile private var loaded: Data? = null

    class Data(
        val categories: List<Category>,
        val chapters: List<Chapter>,
        val morning: List<Zikr>,
        val evening: List<Zikr>,
        val guides: List<Guide> = emptyList(),
    ) {
        val umrahGuide: Guide? get() = guides.firstOrNull { it.key == "umrah" }

        val chapterById: Map<Int, Chapter> = chapters.associateBy { it.id }
        val zikrById: Map<Int, Zikr> =
            (chapters.flatMap { it.items } + morning + evening).associateBy { it.id }
        val chapterOfZikr: Map<Int, Int> = buildMap {
            chapters.forEach { c -> c.items.forEach { put(it.id, c.id) } }
            morning.forEach { put(it.id, -1) }
            evening.forEach { put(it.id, -2) }
        }

        /** Short, self-contained azkar suitable for widgets and reminders. */
        val shortAzkar: List<Zikr> by lazy {
            val pool = morning + evening + listOf(129, 130, 25, 34, 85, 107).flatMap { chapterById[it]?.items.orEmpty() }
            pool.filter { it.ar.length in 12..170 && !it.ar.contains("﴿") }.distinctBy { it.ar }
        }
    }

    fun get(context: Context): Data {
        loaded?.let { return it }
        synchronized(this) {
            loaded?.let { return it }
            val text = context.applicationContext.assets.open("azkar.json").bufferedReader().use { it.readText() }
            return parse(JSONObject(text)).also { loaded = it }
        }
    }

    private fun parseItems(arr: JSONArray): List<Zikr> = List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        Zikr(o.getInt("id"), o.getString("ar"), o.optString("tr"), o.optString("en"), o.optString("fr"), o.optInt("n", 1), Localized.of(o.optJSONObject("ref")))
    }

    private fun parse(root: JSONObject): Data {
        val cats = root.getJSONArray("categories").let { a ->
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                val ids = o.getJSONArray("chapters")
                Category(o.getString("key"), List(ids.length()) { ids.getInt(it) })
            }
        }
        val chapters = root.getJSONArray("chapters").let { a ->
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                Chapter(o.getInt("id"), o.getString("ar"), o.getString("en"), o.getString("fr"), parseItems(o.getJSONArray("items")))
            }
        }
        val guides = root.optJSONArray("guides")?.let { a ->
            List(a.length()) { i ->
                val o = a.getJSONObject(i)
                val steps = o.getJSONArray("steps").let { sa ->
                    List(sa.length()) { j ->
                        val st = sa.getJSONObject(j)
                        val z = st.getJSONArray("zikr")
                        GuideStep(
                            st.optString("icon"), Localized.of(st.getJSONObject("title"))!!, Localized.of(st.getJSONObject("text"))!!,
                            List(z.length()) { z.getInt(it) }, Localized.of(st.optJSONObject("ref")) ?: Localized("", "", ""),
                        )
                    }
                }
                Guide(
                    o.getString("key"), Localized.of(o.getJSONObject("title"))!!, Localized.of(o.getJSONObject("intro"))!!,
                    Localized.of(o.getJSONObject("note"))!!, o.optInt("chapter"), steps,
                )
            }
        }.orEmpty()
        return Data(cats, chapters, parseItems(root.getJSONArray("morning")), parseItems(root.getJSONArray("evening")), guides)
    }
}

private val HARAKAT = Regex("[ً-ٰٟۖ-ۭـ]")

/** Normalises Arabic for searching: strips diacritics and unifies alef / ya / ta marbuta forms. */
fun normalizeArabic(s: String): String = HARAKAT.replace(s, "")
    .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
    .replace('ى', 'ي').replace('ة', 'ه')
    .lowercase()
