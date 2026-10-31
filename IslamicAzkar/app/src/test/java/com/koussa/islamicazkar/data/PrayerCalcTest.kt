package com.koussa.islamicazkar.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Checks the calculator against published timetables (within two minutes). */
class PrayerCalcTest {
    private fun hm(h: Double): String { val m = Math.round(h * 60).toInt(); return "%02d:%02d".format(m / 60, m % 60) }
    private fun minutes(s: String) = s.substringBefore(':').toInt() * 60 + s.substringAfter(':').toInt()
    private fun check(name: String, got: DoubleArray, expected: List<String>) {
        println("$name: " + got.joinToString(" ") { hm(it) })
        got.forEachIndexed { i, h -> assertEquals("$name #$i", minutes(expected[i]).toDouble(), Math.round(h * 60).toDouble(), 2.0) }
    }

    @Test fun makkahUmmAlQura() = check(
        "Makkah 2025-01-01",
        PrayerCalc.hours(2025, 1, 1, 21.4225, 39.8262, 3.0, CalcMethod.UMM_AL_QURA, false),
        listOf("05:38", "06:58", "12:24", "15:28", "17:50", "19:20"),
    )

    @Test fun printSamples() {
        println("Cairo 2025-03-21 EGY: " + PrayerCalc.hours(2025, 3, 21, 30.0444, 31.2357, 2.0, CalcMethod.EGYPT, false).joinToString(" ") { hm(it) })
        println("Beirut 2026-10-31 MWL: " + PrayerCalc.hours(2026, 10, 31, 33.8938, 35.5018, 2.0, CalcMethod.MWL, false).joinToString(" ") { hm(it) })
        println("Paris 2026-06-21 FR: " + PrayerCalc.hours(2026, 6, 21, 48.8566, 2.3522, 2.0, CalcMethod.FRANCE, false).joinToString(" ") { hm(it) })
        println("London 2026-06-21 MWL: " + PrayerCalc.hours(2026, 6, 21, 51.5074, -0.1278, 1.0, CalcMethod.MWL, false).joinToString(" ") { hm(it) })
    }
}
