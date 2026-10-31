package com.koussa.islamicazkar.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/** A place for prayer times: coordinates and the time zone its clocks follow. */
data class Place(val nameAr: String, val name: String, val lat: Double, val lng: Double, val tz: String) {
    fun label(lang: Lang) = if (lang == Lang.AR) nameAr else name

    fun write(e: SharedPreferences.Editor): SharedPreferences.Editor = e
        .putString("place_ar", nameAr).putString("place_name", name)
        .putString("place_lat", lat.toString()).putString("place_lng", lng.toString()).putString("place_tz", tz)

    companion object {
        fun read(sp: SharedPreferences): Place? {
            val lat = sp.getString("place_lat", null)?.toDoubleOrNull() ?: return null
            val lng = sp.getString("place_lng", null)?.toDoubleOrNull() ?: return null
            return Place(
                sp.getString("place_ar", "") ?: "", sp.getString("place_name", "") ?: "",
                lat, lng, sp.getString("place_tz", null) ?: TimeZone.getDefault().id,
            )
        }

        fun clear(e: SharedPreferences.Editor): SharedPreferences.Editor =
            e.remove("place_ar").remove("place_name").remove("place_lat").remove("place_lng").remove("place_tz")
    }
}

/** Cities offered when the phone's location isn't used. */
object Cities {
    val all = listOf(
        Place("مكة المكرمة", "Makkah", 21.4225, 39.8262, "Asia/Riyadh"),
        Place("المدينة المنورة", "Madinah", 24.4672, 39.6112, "Asia/Riyadh"),
        Place("القدس", "Jerusalem", 31.7780, 35.2354, "Asia/Jerusalem"),
        Place("بيروت", "Beirut", 33.8938, 35.5018, "Asia/Beirut"),
        Place("طرابلس (لبنان)", "Tripoli (Lebanon)", 34.4367, 35.8497, "Asia/Beirut"),
        Place("صيدا", "Saida", 33.5571, 35.3729, "Asia/Beirut"),
        Place("دمشق", "Damascus", 33.5138, 36.2765, "Asia/Damascus"),
        Place("عمّان", "Amman", 31.9539, 35.9106, "Asia/Amman"),
        Place("القاهرة", "Cairo", 30.0444, 31.2357, "Africa/Cairo"),
        Place("الرياض", "Riyadh", 24.7136, 46.6753, "Asia/Riyadh"),
        Place("جدة", "Jeddah", 21.5433, 39.1728, "Asia/Riyadh"),
        Place("دبي", "Dubai", 25.2048, 55.2708, "Asia/Dubai"),
        Place("أبوظبي", "Abu Dhabi", 24.4539, 54.3773, "Asia/Dubai"),
        Place("الدوحة", "Doha", 25.2854, 51.5310, "Asia/Qatar"),
        Place("الكويت", "Kuwait City", 29.3759, 47.9774, "Asia/Kuwait"),
        Place("بغداد", "Baghdad", 33.3152, 44.3661, "Asia/Baghdad"),
        Place("إسطنبول", "Istanbul", 41.0082, 28.9784, "Europe/Istanbul"),
        Place("الدار البيضاء", "Casablanca", 33.5731, -7.5898, "Africa/Casablanca"),
        Place("تونس", "Tunis", 36.8065, 10.1815, "Africa/Tunis"),
        Place("الجزائر", "Algiers", 36.7538, 3.0588, "Africa/Algiers"),
        Place("باريس", "Paris", 48.8566, 2.3522, "Europe/Paris"),
        Place("ليون", "Lyon", 45.7640, 4.8357, "Europe/Paris"),
        Place("مرسيليا", "Marseille", 43.2965, 5.3698, "Europe/Paris"),
        Place("بروكسل", "Brussels", 50.8503, 4.3517, "Europe/Brussels"),
        Place("لندن", "London", 51.5074, -0.1278, "Europe/London"),
        Place("برلين", "Berlin", 52.5200, 13.4050, "Europe/Berlin"),
        Place("مونتريال", "Montreal", 45.5019, -73.5674, "America/Toronto"),
        Place("نيويورك", "New York", 40.7128, -74.0060, "America/New_York"),
        Place("سيدني", "Sydney", -33.8688, 151.2093, "Australia/Sydney"),
        Place("كوالالمبور", "Kuala Lumpur", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
        Place("جاكرتا", "Jakarta", -6.2088, 106.8456, "Asia/Jakarta"),
    )
}

/** Calculation authorities: twilight angles for Fajr and Isha (or a fixed delay after Maghrib). */
enum class CalcMethod(val fajr: Double, val isha: Double, val ishaMinutes: Int = 0) {
    MWL(18.0, 17.0),
    UMM_AL_QURA(18.5, 0.0, 90),
    EGYPT(19.5, 17.5),
    ISNA(15.0, 15.0),
    KARACHI(18.0, 18.0),
    GULF(19.5, 0.0, 90),
    DUBAI(18.2, 18.2),
    FRANCE(12.0, 12.0),
    TURKEY(18.0, 17.0),
}

/**
 * Prayer times from the sun's position (the standard method used by most prayer apps):
 * Fajr and Isha at twilight angles, Asr by shadow length, Maghrib at sunset, with the
 * angle-based rule for high latitudes where twilight never ends.
 */
object PrayerCalc {
    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun dtan(d: Double) = tan(Math.toRadians(d))
    private fun darcsin(x: Double) = Math.toDegrees(asin(x))
    private fun darccos(x: Double) = Math.toDegrees(acos(x))
    private fun darctan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun darccot(x: Double) = Math.toDegrees(atan(1 / x))
    private fun fix(a: Double, b: Double) = a - b * floor(a / b)

    private fun julian(year: Int, month: Int, day: Int): Double {
        var y = year; var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /** Declination and equation of time for a Julian date. */
    private fun sun(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fix(357.529 + 0.98560028 * d, 360.0)
        val q = fix(280.459 + 0.98564736 * d, 360.0)
        val l = fix(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g), 360.0)
        val e = 23.439 - 0.00000036 * d
        val ra = fix(darctan2(dcos(e) * dsin(l), dcos(l)) / 15, 24.0)
        val eqt = q / 15 - fix(ra, 24.0)
        val decl = darcsin(dsin(e) * dsin(l))
        return decl to eqt
    }

    /** Fajr, sunrise, Dhuhr, Asr, Maghrib and Isha in local hours for the given day. */
    fun hours(year: Int, month: Int, day: Int, lat: Double, lng: Double, tz: Double, method: CalcMethod, hanafi: Boolean): DoubleArray {
        val jd = julian(year, month, day) - lng / (15 * 24.0)
        fun midDay(t: Double) = fix(12 - sun(jd + t).second, 24.0)
        fun angleTime(angle: Double, t: Double, ccw: Boolean): Double {
            val decl = sun(jd + t).first
            val x = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))
            val span = darccos(x) / 15
            return midDay(t) + if (ccw) -span else span
        }
        fun asr(factor: Int, t: Double): Double {
            val decl = sun(jd + t).first
            return angleTime(-darccot(factor + dtan(abs(lat - decl))), t, false)
        }

        var fajr = angleTime(method.fajr, 5 / 24.0, true)
        val sunrise = angleTime(0.833, 6 / 24.0, true)
        val dhuhr = midDay(12 / 24.0)
        val asr = asr(if (hanafi) 2 else 1, 13 / 24.0)
        val sunset = angleTime(0.833, 18 / 24.0, false)
        var isha = if (method.ishaMinutes > 0) Double.NaN else angleTime(method.isha, 18 / 24.0, false)

        // High latitudes: when twilight lasts all night, use a portion of the night.
        val night = 24 - (sunset - sunrise)
        val fajrLimit = night * method.fajr / 60
        if (fajr.isNaN() || sunrise - fajr > fajrLimit) fajr = sunrise - fajrLimit
        if (method.ishaMinutes > 0) {
            isha = sunset + method.ishaMinutes / 60.0
        } else {
            val ishaLimit = night * method.isha / 60
            if (isha.isNaN() || isha - sunset > ishaLimit) isha = sunset + ishaLimit
        }

        val shift = tz - lng / 15
        return doubleArrayOf(fajr, sunrise, dhuhr, asr, sunset, isha).map { it + shift }.toDoubleArray()
    }
}

/** Today's prayer times for the saved place, as epoch milliseconds. */
object Prayers {
    const val FAJR = 0
    const val SUNRISE = 1
    const val ISHA = 5

    fun place(ctx: Context): Place? = Place.read(Settings.prefs(ctx))

    fun method(ctx: Context): CalcMethod = CalcMethod.entries.getOrElse(Settings.prefs(ctx).getInt(Settings.K_METHOD, 0)) { CalcMethod.MWL }

    /** The six times (Fajr…Isha) of the day containing [day], in the place's time zone. */
    fun times(ctx: Context, place: Place, day: Long = System.currentTimeMillis()): LongArray {
        val zone = TimeZone.getTimeZone(place.tz)
        val c = Calendar.getInstance(zone).apply { timeInMillis = day }
        val y = c.get(Calendar.YEAR); val m = c.get(Calendar.MONTH) + 1; val d = c.get(Calendar.DAY_OF_MONTH)
        c.set(y, m - 1, d, 0, 0, 0); c.set(Calendar.MILLISECOND, 0)
        val midnight = c.timeInMillis
        val tz = zone.getOffset(midnight + 12 * 3_600_000L) / 3_600_000.0
        val hanafi = Settings.prefs(ctx).getBoolean(Settings.K_ASR, false)
        val h = PrayerCalc.hours(y, m, d, place.lat, place.lng, tz, method(ctx), hanafi)
        // Round to the minute, as printed timetables do.
        return LongArray(6) { i -> midnight + Math.round(h[i] * 60) * 60_000L }
    }

    /** The next prayer (sunrise excluded) after [now]: its index and time, rolling over to tomorrow's Fajr. */
    fun next(ctx: Context, place: Place, now: Long = System.currentTimeMillis()): Pair<Int, Long> {
        val t = times(ctx, place, now)
        for (i in 0..5) if (i != SUNRISE && t[i] > now) return i to t[i]
        return FAJR to times(ctx, place, now + 86_400_000L)[FAJR]
    }

    /** The prayer whose time it currently is (for progress rings), with its start. */
    fun previous(ctx: Context, place: Place, now: Long = System.currentTimeMillis()): Pair<Int, Long> {
        val t = times(ctx, place, now)
        for (i in 5 downTo 0) if (i != SUNRISE && t[i] <= now) return i to t[i]
        return ISHA to times(ctx, place, now - 86_400_000L)[ISHA]
    }
}
