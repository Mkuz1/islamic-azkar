package com.koussa.islamicazkar.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * App preferences backed by SharedPreferences and mirrored into Compose state, so
 * the UI recomposes immediately while widgets, receivers and alarms can read the
 * same values without the UI running.
 */
class Settings private constructor(private val sp: SharedPreferences) {

    var lang by mutableStateOf(Lang.of(sp.getString(K_LANG, null)))
        private set
    var theme by mutableStateOf(ThemeMode.valueOf(sp.getString(K_THEME, ThemeMode.SYSTEM.name)!!))
        private set
    var textScale by mutableStateOf(sp.getFloat(K_SCALE, 1f))
        private set
    var showTranslit by mutableStateOf(sp.getBoolean(K_TRANSLIT, true))
        private set
    var showMeaning by mutableStateOf(sp.getBoolean(K_MEANING, true))
        private set
    var haptics by mutableStateOf(sp.getBoolean(K_HAPTICS, true))
        private set
    var favorites by mutableStateOf(sp.getStringSet(K_FAVS, emptySet())!!.mapNotNull { it.toIntOrNull() }.toSet())
        private set

    // Reminders. Times are minutes after midnight.
    var morningOn by mutableStateOf(sp.getBoolean(K_MORNING_ON, false)); private set
    var morningAt by mutableStateOf(sp.getInt(K_MORNING_AT, 6 * 60)); private set
    var eveningOn by mutableStateOf(sp.getBoolean(K_EVENING_ON, false)); private set
    var eveningAt by mutableStateOf(sp.getInt(K_EVENING_AT, 17 * 60)); private set
    var generalOn by mutableStateOf(sp.getBoolean(K_GENERAL_ON, false)); private set
    var generalEveryHours by mutableStateOf(sp.getInt(K_GENERAL_EVERY, 3)); private set
    var generalFrom by mutableStateOf(sp.getInt(K_GENERAL_FROM, 9 * 60)); private set
    var generalTo by mutableStateOf(sp.getInt(K_GENERAL_TO, 21 * 60)); private set

    // Tasbih
    var tasbihCount by mutableStateOf(sp.getInt(K_TASBIH_COUNT, 0)); private set
    var tasbihTarget by mutableStateOf(sp.getInt(K_TASBIH_TARGET, 33)); private set
    var tasbihPhrase by mutableStateOf(sp.getInt(K_TASBIH_PHRASE, 0)); private set
    var tasbihTotal by mutableStateOf(sp.getInt(K_TASBIH_TOTAL, 0)); private set

    // Widgets, calendar and prayer times
    var palette by mutableStateOf(sp.getInt(K_PALETTE, 0)); private set
    var script by mutableStateOf(sp.getInt(K_SCRIPT, 0)); private set
    var hijriOffset by mutableStateOf(sp.getInt(K_HIJRI_OFFSET, 0)); private set
    var lockGlance by mutableStateOf(sp.getBoolean(K_LOCK_GLANCE, false)); private set
    var place by mutableStateOf(Place.read(sp)); private set
    var method by mutableStateOf(sp.getInt(K_METHOD, 0)); private set
    var asrHanafi by mutableStateOf(sp.getBoolean(K_ASR, false)); private set

    fun updatePalette(v: Int) { palette = v; sp.edit().putInt(K_PALETTE, v).commit() }
    fun updateScript(v: Int) { script = v; sp.edit().putInt(K_SCRIPT, v).commit() }
    fun updateHijriOffset(v: Int) { hijriOffset = v; sp.edit().putInt(K_HIJRI_OFFSET, v).commit() }
    fun updateLockGlance(v: Boolean) { lockGlance = v; sp.edit().putBoolean(K_LOCK_GLANCE, v).commit() }
    fun updatePlace(v: Place?) { place = v; (if (v == null) Place.clear(sp.edit()) else v.write(sp.edit())).commit() }
    fun updateMethod(v: Int) { method = v; sp.edit().putInt(K_METHOD, v).commit() }
    fun updateAsrHanafi(v: Boolean) { asrHanafi = v; sp.edit().putBoolean(K_ASR, v).commit() }

    fun updateLang(v: Lang) { lang = v; sp.edit().putString(K_LANG, v.code).commit() }
    fun updateTheme(v: ThemeMode) { theme = v; sp.edit().putString(K_THEME, v.name).apply() }
    fun updateTextScale(v: Float) { textScale = v; sp.edit().putFloat(K_SCALE, v).apply() }
    fun updateShowTranslit(v: Boolean) { showTranslit = v; sp.edit().putBoolean(K_TRANSLIT, v).apply() }
    fun updateShowMeaning(v: Boolean) { showMeaning = v; sp.edit().putBoolean(K_MEANING, v).apply() }
    fun updateHaptics(v: Boolean) { haptics = v; sp.edit().putBoolean(K_HAPTICS, v).apply() }

    fun isFavorite(id: Int) = id in favorites
    fun toggleFavorite(id: Int) {
        favorites = if (id in favorites) favorites - id else favorites + id
        sp.edit().putStringSet(K_FAVS, favorites.map { it.toString() }.toSet()).apply()
    }

    fun setMorning(on: Boolean = morningOn, at: Int = morningAt) {
        morningOn = on; morningAt = at
        sp.edit().putBoolean(K_MORNING_ON, on).putInt(K_MORNING_AT, at).apply()
    }
    fun setEvening(on: Boolean = eveningOn, at: Int = eveningAt) {
        eveningOn = on; eveningAt = at
        sp.edit().putBoolean(K_EVENING_ON, on).putInt(K_EVENING_AT, at).apply()
    }
    fun setGeneral(on: Boolean = generalOn, everyHours: Int = generalEveryHours, from: Int = generalFrom, to: Int = generalTo) {
        generalOn = on; generalEveryHours = everyHours; generalFrom = from; generalTo = to
        sp.edit().putBoolean(K_GENERAL_ON, on).putInt(K_GENERAL_EVERY, everyHours)
            .putInt(K_GENERAL_FROM, from).putInt(K_GENERAL_TO, to).apply()
    }

    fun setTasbih(count: Int = tasbihCount, target: Int = tasbihTarget, phrase: Int = tasbihPhrase, total: Int = tasbihTotal) {
        tasbihCount = count; tasbihTarget = target; tasbihPhrase = phrase; tasbihTotal = total
        sp.edit().putInt(K_TASBIH_COUNT, count).putInt(K_TASBIH_TARGET, target)
            .putInt(K_TASBIH_PHRASE, phrase).putInt(K_TASBIH_TOTAL, total).apply()
    }

    /**
     * Records that [key] (e.g. "morning") was completed today and returns the number of
     * consecutive days it has been completed, including today.
     */
    fun recordCompletion(key: String): Int {
        val today = (System.currentTimeMillis() + java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis())) / 86_400_000L
        val last = sp.getLong("streak_day_$key", Long.MIN_VALUE)
        val count = sp.getInt("streak_$key", 0)
        val next = when (today - last) {
            0L -> maxOf(count, 1)
            1L -> count + 1
            else -> 1
        }
        sp.edit().putLong("streak_day_$key", today).putInt("streak_$key", next).apply()
        return next
    }

    /** Re-reads values another component (e.g. the tasbih widget) may have written. */
    fun reloadTasbih() {
        tasbihCount = sp.getInt(K_TASBIH_COUNT, 0)
        tasbihTarget = sp.getInt(K_TASBIH_TARGET, 33)
        tasbihPhrase = sp.getInt(K_TASBIH_PHRASE, 0)
        tasbihTotal = sp.getInt(K_TASBIH_TOTAL, 0)
    }

    companion object {
        private const val K_LANG = "lang"
        private const val K_THEME = "theme"
        private const val K_SCALE = "text_scale"
        private const val K_TRANSLIT = "translit"
        private const val K_MEANING = "meaning"
        private const val K_HAPTICS = "haptics"
        private const val K_FAVS = "favorites"
        private const val K_MORNING_ON = "morning_on"
        private const val K_MORNING_AT = "morning_at"
        private const val K_EVENING_ON = "evening_on"
        private const val K_EVENING_AT = "evening_at"
        private const val K_GENERAL_ON = "general_on"
        private const val K_GENERAL_EVERY = "general_every"
        private const val K_GENERAL_FROM = "general_from"
        private const val K_GENERAL_TO = "general_to"
        const val K_PALETTE = "widget_palette"
        const val K_SCRIPT = "calligraphy_script"
        const val K_HIJRI_OFFSET = "hijri_offset"
        const val K_LOCK_GLANCE = "lock_glance"
        const val K_METHOD = "prayer_method"
        const val K_ASR = "asr_hanafi"
        const val K_TASBIH_COUNT = "tasbih_count"
        const val K_TASBIH_TARGET = "tasbih_target"
        const val K_TASBIH_PHRASE = "tasbih_phrase"
        const val K_TASBIH_TOTAL = "tasbih_total"

        @Volatile private var instance: Settings? = null

        fun get(context: Context): Settings = instance ?: synchronized(this) {
            instance ?: Settings(prefs(context)).also { instance = it }
        }

        fun prefs(context: Context): SharedPreferences =
            context.applicationContext.getSharedPreferences("islamic_azkar", Context.MODE_PRIVATE)
    }
}

/** The tasbih phrases offered by the counter and its widget. */
object TasbihPhrases {
    val ar = listOf("سُبْحَانَ اللَّهِ", "الْحَمْدُ لِلَّهِ", "اللَّهُ أَكْبَرُ", "لَا إِلَهَ إِلَّا اللَّهُ", "أَسْتَغْفِرُ اللَّهَ", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ")
    val en = listOf("Glory be to Allah", "All praise is for Allah", "Allah is the Greatest", "None is worthy of worship but Allah", "I seek Allah's forgiveness", "Glory and praise be to Allah", "No might nor power except with Allah", "O Allah, bless Muhammad")
    val fr = listOf("Gloire à Allah", "Louange à Allah", "Allah est le Plus Grand", "Nul ne mérite d'être adoré sauf Allah", "Je demande pardon à Allah", "Gloire et louange à Allah", "Nulle force ni puissance sauf par Allah", "Ô Allah, prie sur Muhammad")

    fun meaning(i: Int, lang: Lang) = when (lang) { Lang.AR -> ""; Lang.EN -> en[i]; Lang.FR -> fr[i] }
}
