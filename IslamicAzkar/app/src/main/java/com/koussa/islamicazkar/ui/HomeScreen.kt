package com.koussa.islamicazkar.ui

import android.icu.text.SimpleDateFormat
import android.icu.util.ULocale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koussa.islamicazkar.data.AzkarRepository
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Reading
import com.koussa.islamicazkar.data.normalizeArabic
import com.koussa.islamicazkar.i18n.category
import com.koussa.islamicazkar.i18n.num
import com.koussa.islamicazkar.ui.glass.ArabicFont
import com.koussa.islamicazkar.ui.glass.Glass
import com.koussa.islamicazkar.ui.glass.pressable
import com.koussa.islamicazkar.ui.glass.GlassSegmented
import java.util.Calendar
import java.util.Date

enum class Moment { MORNING, NOON, EVENING, NIGHT }

fun currentMoment(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): Moment = when (hour) {
    in 4..11 -> Moment.MORNING
    in 12..14 -> Moment.NOON
    in 15..19 -> Moment.EVENING
    else -> Moment.NIGHT
}

fun hijriDate(lang: Lang): String = runCatching {
    SimpleDateFormat("d MMMM y", ULocale("${lang.code}@calendar=islamic-umalqura")).format(Date())
}.getOrDefault("")

@Composable
fun HomeScreen() {
    val app = LocalApp.current
    val ctx = LocalContext.current
    val data = remember { AzkarRepository.get(ctx) }
    val g = Glass.colors
    val s = Glass.s
    val lang = Glass.lang
    var query by rememberSaveable { mutableStateOf("") }
    val moment = remember { currentMoment() }
    val listState = rememberLazyListState()
    CollapsingTitle(s.appName, listState)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = screenPadding()) {
            item {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            app.settings.hijriOffset.let { com.koussa.islamicazkar.data.Hijri.format(lang, com.koussa.islamicazkar.data.Hijri.of(ctx)) }, color = g.text2, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        LargeTitle(s.appName)
                    }
                    Box(Modifier.width(124.dp).padding(bottom = 14.dp)) {
                        GlassSegmented(listOf("ع", "EN", "FR"), Lang.entries.indexOf(lang)) { app.settings.updateLang(Lang.entries[it]) }
                    }
                }
            }
            item { SearchBar(query, s.searchHint) { query = it } }

            if (query.isBlank()) {
                app.settings.place?.let { place -> item { NextPrayerCard(place) } }
                item { SectionHeader(s.recommendedNow) }
                item {
                    val tiles = listOf(
                        Tile(Reading.Morning, s.morningAzkar, Icons.Rounded.WbSunny, Color(0xFFFF9500), data.morning.size, moment == Moment.MORNING),
                        Tile(Reading.Evening, s.eveningAzkar, Icons.Rounded.NightsStay, Color(0xFF5856D6), data.evening.size, moment == Moment.EVENING),
                        Tile(Reading.OfChapter(28), s.sleepAzkar, Icons.Rounded.Bedtime, Color(0xFF007AFF), data.chapterById[28]?.items?.size ?: 0, moment == Moment.NIGHT),
                        Tile(Reading.OfChapter(25), s.afterPrayer, Icons.Rounded.Mosque, Color(0xFF34C759), data.chapterById[25]?.items?.size ?: 0, moment == Moment.NOON),
                    )
                    val rows = if (wide) listOf(tiles) else tiles.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        rows.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { t -> TodayCard(t, Modifier.weight(1f)) { app.push(Route.Reader(t.reading)) } }
                            }
                        }
                    }
                }

                item { SectionHeader(s.categories) }
                item {
                    InsetGroup {
                        data.categories.forEachIndexed { i, cat ->
                            if (i > 0) RowDivider()
                            ListRow(
                                title = s.category(cat.key),
                                icon = categoryIcon(cat.key), iconColor = categoryHue(cat.key),
                                value = lang.num(cat.chapterIds.size),
                                chevron = true,
                                onClick = { app.push(Route.Category(cat.key)) },
                            )
                        }
                    }
                }
                data.umrahGuide?.let { guide ->
                    item { SectionHeader(s.guide) }
                    item { InsetGroup { GuideRow(guide) } }
                }
                item {
                    Text(
                        DEDICATION_AR,
                        fontFamily = ArabicFont, color = g.text3, fontSize = 15.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                    )
                }
            } else {
                val q = normalizeArabic(query.trim())
                val chapterHits = data.chapters.filter { c ->
                    normalizeArabic(c.ar).contains(q) || c.en.lowercase().contains(q) || c.fr.lowercase().contains(q)
                }.take(30)
                val zikrHits = data.chapters.flatMap { c -> c.items.map { it to c } }.filter { (z, _) ->
                    normalizeArabic(z.ar).contains(q) || z.en.lowercase().contains(q) || z.fr.lowercase().contains(q)
                }.take(40)
                if (chapterHits.isEmpty() && zikrHits.isEmpty()) {
                    item { Text(s.noResults, color = g.text2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(40.dp)) }
                }
                if (chapterHits.isNotEmpty()) {
                    item { SectionHeader(s.categories) }
                    item {
                        InsetGroup {
                            chapterHits.forEachIndexed { i, c ->
                                if (i > 0) RowDivider(16.dp)
                                ListRow(
                                    title = c.title(lang), subtitle = if (lang == Lang.AR) null else c.ar, subtitleFont = ArabicFont,
                                    chevron = true, onClick = { app.push(Route.Reader(Reading.OfChapter(c.id))) },
                                )
                            }
                        }
                    }
                }
                if (zikrHits.isNotEmpty()) {
                    item { SectionHeader(s.azkar(zikrHits.size)) }
                    items(zikrHits, key = { "z" + it.first.id }) { (z, c) ->
                        Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = { app.push(Route.Reader(Reading.OfChapter(c.id), z.id)) }) {
                            Text(c.title(lang), color = g.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                z.ar, fontFamily = ArabicFont, fontSize = 19.sp, lineHeight = 34.sp, color = g.text, maxLines = 3,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Tile(val reading: Reading, val title: String, val icon: ImageVector, val color: Color, val count: Int, val now: Boolean)

/** A summary card in the style of the Health / Fitness apps. */
@Composable
private fun TodayCard(t: Tile, modifier: Modifier, onClick: () -> Unit) {
    val g = Glass.colors
    val s = Glass.s
    Card(modifier, onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SymbolSquare(t.icon, t.color, 32.dp)
            Spacer(Modifier.weight(1f))
            if (t.now) Capsule(s.now, color = g.accent, background = g.accentSoft)
        }
        Spacer(Modifier.height(14.dp))
        Text(t.title, color = g.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(s.azkar(t.count), color = g.text2, fontSize = 13.sp)
    }
}

/** A chapter row used in category lists and search. */
@Composable
fun ChapterRow(title: String, subtitle: String?, meta: String, number: Int, onClick: () -> Unit) {
    ListRow(title = title, subtitle = subtitle, subtitleFont = ArabicFont, value = meta, chevron = true, onClick = onClick)
}

/** The next prayer with a live countdown; tapping it opens the timetable in Settings. */
@Composable
private fun NextPrayerCard(place: com.koussa.islamicazkar.data.Place) {
    val app = LocalApp.current
    val ctx = LocalContext.current
    val g = Glass.colors
    val lang = Glass.lang
    val m = com.koussa.islamicazkar.i18n.more(lang)
    var now by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1_000); now = System.currentTimeMillis() } }
    val st = app.settings
    val (next, at) = remember(place, now / 60_000, st.method, st.asrHanafi) { com.koussa.islamicazkar.data.Prayers.next(ctx, place, now) }
    val left = ((at - now) / 1000).coerceAtLeast(0)
    val countdown = lang.num((left / 3600).toInt()) + ":" + lang.num(((left / 60) % 60).toInt()).padStart(2, if (lang == Lang.AR) '٠' else '0') +
        ":" + lang.num((left % 60).toInt()).padStart(2, if (lang == Lang.AR) '٠' else '0')
    Card(Modifier.padding(top = 14.dp).pressable(onClick = { app.tab = 4 })) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            SymbolSquare(Icons.Rounded.Mosque, Color(0xFF34C759))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(m.nextPrayer, color = g.text2, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(com.koussa.islamicazkar.i18n.more(Lang.AR).prayers[next], fontFamily = com.koussa.islamicazkar.ui.glass.CalligraphyFont, fontSize = 26.sp, color = g.accent)
                    if (lang != Lang.AR) Text("  " + m.prayers[next], color = g.text, fontSize = 16.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(com.koussa.islamicazkar.widget.prayerClock(lang, at, place.tz), color = g.text, fontSize = 24.sp, fontWeight = FontWeight.Light)
                Text(when (lang) { Lang.AR -> "بعد $countdown"; Lang.EN -> "in $countdown"; Lang.FR -> "dans $countdown" }, color = g.text2, fontSize = 13.sp)
            }
        }
    }
}
