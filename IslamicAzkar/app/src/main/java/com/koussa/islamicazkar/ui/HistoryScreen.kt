package com.koussa.islamicazkar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koussa.islamicazkar.data.AzkarHistory
import com.koussa.islamicazkar.data.AzkarRepository
import com.koussa.islamicazkar.data.Hijri
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Reading
import com.koussa.islamicazkar.data.digits
import com.koussa.islamicazkar.i18n.category
import com.koussa.islamicazkar.ui.glass.Glass
import com.koussa.islamicazkar.ui.glass.pressable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Texts for the Record tab (same wording as the iPhone app). */
class HistoryTexts(
    val tab: String, val title: String, val subtitle: String, val activeDays: String, val finished: String,
    val streak: String, val nothing: String, val completed: String, val today: String,
) {
    companion object {
        fun of(lang: Lang) = when (lang) {
            Lang.AR -> HistoryTexts("سجلّي", "سجلّ الأذكار", "الأذكار التي أتممتها كل يوم", "أيام الذكر هذا الشهر",
                "أذكار أتممتها هذا الشهر", "أيام متتالية", "لم تُتمّ أذكارًا في هذا اليوم.", "الأذكار المكتملة", "اليوم")
            Lang.EN -> HistoryTexts("Record", "Azkar record", "The azkar you completed each day", "Days with azkar this month",
                "Azkar completed this month", "Days in a row", "No azkar completed on this day.", "Azkar completed", "Today")
            Lang.FR -> HistoryTexts("Historique", "Historique des adhkar", "Les adhkar terminés chaque jour", "Jours avec des adhkar ce mois-ci",
                "Adhkar terminés ce mois-ci", "Jours d'affilée", "Aucun dhikr terminé ce jour-là.", "Adhkar terminés", "Aujourd'hui")
        }
    }
}

private fun startOfMonth(c: Calendar): Calendar = (c.clone() as Calendar).apply {
    set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}

private fun locale(lang: Lang) = Locale(lang.code)

/** The Record tab: a month calendar of completed azkar, the month's totals, and the selected day's list. */
@Composable
fun HistoryScreen() {
    val ctx = LocalContext.current
    val app = LocalApp.current
    val g = Glass.colors
    val lang = Glass.lang
    val t = HistoryTexts.of(lang)
    val version by AzkarHistory.version
    val days = remember(version) { AzkarHistory.all(ctx) }

    var monthOffset by rememberSaveable { mutableIntStateOf(0) }    // 0 = this month, -1 = last month…
    var selectedKey by rememberSaveable { mutableStateOf(AzkarHistory.dayKey()) }
    val month = startOfMonth(Calendar.getInstance()).apply { add(Calendar.MONTH, monthOffset) }
    val prefix = "%04d-%02d-".format(month.get(Calendar.YEAR), month.get(Calendar.MONTH) + 1)
    val inMonth = days.filterKeys { it.startsWith(prefix) }

    val listState = rememberLazyListState()
    CollapsingTitle(t.title, listState)

    LazyColumn(
        Modifier.fillMaxSize(), state = listState,
        contentPadding = screenPadding(), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { LargeTitle(t.title, t.subtitle) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat(lang.digits(inMonth.values.count { it.isNotEmpty() }), t.activeDays, Icons.Rounded.CalendarMonth, Modifier.weight(1f))
                Stat(lang.digits(inMonth.values.sumOf { it.size }), t.finished, Icons.Rounded.TaskAlt, Modifier.weight(1f))
                Stat(lang.digits(AzkarHistory.currentStreak(days)), t.streak, Icons.Rounded.LocalFireDepartment, Modifier.weight(1f))
            }
        }
        item {
            Card {
                MonthHeader(month, lang, canGoForward = monthOffset < 0,
                    onPrev = { monthOffset-- }, onNext = { if (monthOffset < 0) monthOffset++ })
                Spacer(Modifier.height(10.dp))
                MonthGrid(month, days, selectedKey) { selectedKey = it }
            }
        }
        item {
            val readings = days[selectedKey].orEmpty().mapNotNull { Reading.fromKey(it) }
            val sel = Calendar.getInstance().apply {
                val p = selectedKey.split("-").map { it.toInt() }
                set(p[0], p[1] - 1, p[2], 12, 0, 0)
            }
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val title = if (selectedKey == AzkarHistory.dayKey()) t.today
                        else SimpleDateFormat("EEEE d MMMM y", locale(lang)).format(sel.time)
                    Text(title, color = g.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(Hijri.format(lang, Hijri.of(ctx, sel.timeInMillis)), color = g.text2, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                if (readings.isEmpty()) {
                    Text(t.nothing, color = g.text2, fontSize = 15.sp, modifier = Modifier.padding(vertical = 6.dp))
                } else {
                    val head = "${t.completed} (${lang.digits(readings.size)})"
                    Text(if (lang == Lang.AR) head else head.uppercase(), color = g.text2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    readings.forEachIndexed { i, r ->
                        ReadingRow(r) { app.push(Route.Reader(r)) }
                        if (i < readings.lastIndex) RowDivider(inset = 44.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, icon: ImageVector, modifier: Modifier) {
    val g = Glass.colors
    Card(modifier.heightIn(min = 104.dp), padding = 12.dp) {
        Icon(icon, null, tint = g.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = g.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = g.text2, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 3)
    }
}

@Composable
private fun MonthHeader(month: Calendar, lang: Lang, canGoForward: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    val ctx = LocalContext.current
    val g = Glass.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(   // auto-mirrored: points the right way in Arabic
            Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null,
            tint = g.accent, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(50)).pressable(onPrev).padding(4.dp),
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            val name = SimpleDateFormat("LLLL", locale(lang)).format(month.time)
            Text("$name ${lang.digits(month.get(Calendar.YEAR))}", color = g.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(Hijri.format(lang, Hijri.of(ctx, month.timeInMillis)), color = g.text2, fontSize = 12.sp)
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight, null,
            tint = if (canGoForward) g.accent else g.text3,
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(50)).pressable(onNext, enabled = canGoForward).padding(4.dp),
        )
    }
}

private val WEEK_AR = listOf("أحد", "اثن", "ثلا", "أرب", "خمي", "جمع", "سبت")
private val WEEK_EN = listOf("S", "M", "T", "W", "T", "F", "S")
private val WEEK_FR = listOf("D", "L", "M", "M", "J", "V", "S")

@Composable
private fun MonthGrid(month: Calendar, days: Map<String, List<String>>, selectedKey: String, onSelect: (String) -> Unit) {
    val g = Glass.colors
    val lang = Glass.lang
    val first = Calendar.getInstance().firstDayOfWeek    // Calendar.SUNDAY = 1
    val lead = (month.get(Calendar.DAY_OF_WEEK) - first + 7) % 7
    val count = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val names = when (lang) { Lang.AR -> WEEK_AR; Lang.EN -> WEEK_EN; Lang.FR -> WEEK_FR }
    val todayKey = AzkarHistory.dayKey()
    val now = System.currentTimeMillis()

    Row {
        for (i in 0 until 7) {
            Text(names[(first - 1 + i) % 7], color = g.text2, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(6.dp))
    val cells = List(lead) { null } + (1..count).toList()
    cells.chunked(7).forEach { week ->
        Row(Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0 until 7) {
                val day = week.getOrNull(i)
                Box(Modifier.weight(1f).aspectRatio(0.95f)) {
                    if (day != null) {
                        val c = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day); set(Calendar.HOUR_OF_DAY, 0) }
                        val key = AzkarHistory.dayKey(c)
                        val readings = days[key].orEmpty()
                        val future = c.timeInMillis > now
                        val n = readings.size
                        val shade = when { n == 0 -> 0f; n < 3 -> 0.25f; n < 6 -> 0.5f; else -> 0.8f }
                        val shape = RoundedCornerShape(10.dp)
                        Column(
                            Modifier.fillMaxSize().clip(shape)
                                .background(g.accent.copy(alpha = shade))
                                .border(
                                    width = if (key == selectedKey) 2.dp else 1.5.dp,
                                    color = when { key == selectedKey -> g.gold; key == todayKey -> g.accent; else -> Color.Transparent },
                                    shape = shape,
                                )
                                .pressable({ onSelect(key) }, enabled = !future),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                lang.digits(day), fontSize = 15.sp,
                                fontWeight = if (key == todayKey) FontWeight.Bold else FontWeight.Normal,
                                color = when { shade >= 0.5f -> Color.White; future -> g.text3; else -> g.text },
                            )
                            Row(Modifier.height(10.dp), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                if ("morning" in readings) Icon(Icons.Rounded.WbSunny, null, tint = Color(0xFFFF9500), modifier = Modifier.size(9.dp))
                                if ("evening" in readings) Icon(Icons.Rounded.DarkMode, null, tint = Color(0xFF5856D6), modifier = Modifier.size(9.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One completed reading: its category's icon, its title and the category it belongs to. */
@Composable
private fun ReadingRow(r: Reading, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val g = Glass.colors
    val s = Glass.s
    val lang = Glass.lang
    val data = remember { AzkarRepository.get(ctx) }
    val (icon, hue, cat) = when (r) {
        Reading.Morning -> Triple(Icons.Rounded.WbSunny, Color(0xFFFF9500), null)
        Reading.Evening -> Triple(Icons.Rounded.DarkMode, Color(0xFF5856D6), null)
        is Reading.OfChapter -> data.categories.firstOrNull { r.id in it.chapterIds }
            ?.let { Triple(categoryIcon(it.key), categoryHue(it.key), s.category(it.key)) }
            ?: Triple(categoryIcon(""), categoryHue(""), null)
    }
    val title = when (r) {
        Reading.Morning -> s.morningAzkar
        Reading.Evening -> s.eveningAzkar
        is Reading.OfChapter -> data.chapterById[r.id]?.title(lang) ?: ""
    }
    Row(
        Modifier.fillMaxWidth().pressable(onClick).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolSquare(icon, hue, 32.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = g.text, fontSize = 16.sp, maxLines = 2)
            if (cat != null) Text(cat, color = g.text2, fontSize = 12.sp)
        }
        Icon(Icons.Rounded.CheckCircle, null, tint = g.accent, modifier = Modifier.size(20.dp))
    }
}
