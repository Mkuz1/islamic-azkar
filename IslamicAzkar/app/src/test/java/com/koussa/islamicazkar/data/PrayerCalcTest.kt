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

    /** Dubai (Dulook DXB / IACAD): aladhan.com method 16 for Dubai, fetched 2026-11-05. */
    @Test fun dubaiDulook() {
        val days = mapOf(
            Triple(2026, 1, 1) to "05:42 07:04 12:25 15:20 17:44 19:03",
            Triple(2026, 3, 21) to "05:05 06:22 12:29 15:52 18:33 19:48",
            Triple(2026, 6, 21) to "03:59 05:29 12:24 15:43 19:15 20:43",
            Triple(2026, 9, 23) to "04:51 06:08 12:14 15:39 18:17 19:32",
            Triple(2026, 11, 5) to "05:10 06:29 12:05 15:14 17:39 18:55",
            Triple(2026, 12, 21) to "05:37 07:00 12:20 15:14 17:37 18:57",
        )
        for ((d, times) in days) {
            val got = PrayerCalc.hours(d.first, d.second, d.third, 25.2048, 55.2708, 4.0, CalcMethod.DUBAI, false)
            got.forEachIndexed { i, h ->
                assertEquals("Dubai $d #$i", minutes(times.split(' ')[i]).toDouble(), Math.round(h * 60).toDouble(), 1.0)
            }
        }
    }

    @Test fun printSamples() {
        println("Cairo 2025-03-21 EGY: " + PrayerCalc.hours(2025, 3, 21, 30.0444, 31.2357, 2.0, CalcMethod.EGYPT, false).joinToString(" ") { hm(it) })
        println("Beirut 2026-10-31 MWL: " + PrayerCalc.hours(2026, 10, 31, 33.8938, 35.5018, 2.0, CalcMethod.MWL, false).joinToString(" ") { hm(it) })
        println("Paris 2026-06-21 FR: " + PrayerCalc.hours(2026, 6, 21, 48.8566, 2.3522, 2.0, CalcMethod.FRANCE, false).joinToString(" ") { hm(it) })
        println("London 2026-06-21 MWL: " + PrayerCalc.hours(2026, 6, 21, 51.5074, -0.1278, 1.0, CalcMethod.MWL, false).joinToString(" ") { hm(it) })
    }
}
