package com.koussa.islamicazkar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koussa.islamicazkar.data.AzkarRepository
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Reading
import com.koussa.islamicazkar.data.Zikr
import com.koussa.islamicazkar.i18n.category
import com.koussa.islamicazkar.i18n.num
import com.koussa.islamicazkar.ui.glass.AccentButton
import com.koussa.islamicazkar.ui.glass.Glass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CategoryScreen(key: String) {
    val app = LocalApp.current
    val ctx = LocalContext.current
    val data = remember { AzkarRepository.get(ctx) }
    val s = Glass.s
    val lang = Glass.lang
    val cat = data.categories.first { it.key == key }
    val chapters = cat.chapterIds.mapNotNull { data.chapterById[it] }
    val listState = rememberLazyListState()
    CollapsingTitle(s.category(key), listState)

    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = screenPadding(hasBack = true)) {
        item { LargeTitle(s.category(key), s.chapters(chapters.size)) }
        val guide = data.umrahGuide
        if (key == "hajj" && guide != null) item { InsetGroup(Modifier.padding(bottom = 14.dp)) { GuideRow(guide) } }
        item {
            InsetGroup {
                chapters.forEachIndexed { i, c ->
                    if (i > 0) RowDivider(16.dp)
                    ChapterRow(c.title(lang), if (lang == Lang.AR) null else c.ar, lang.num(c.items.size), c.id) {
                        app.push(Route.Reader(Reading.OfChapter(c.id)))
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderScreen(reading: Reading, focus: Int?) {
    val app = LocalApp.current
    val ctx = LocalContext.current
    val view = LocalView.current
    val data = remember { AzkarRepository.get(ctx) }
    val g = Glass.colors
    val s = Glass.s
    val lang = Glass.lang

    val (title, subtitle, items) = when (reading) {
        Reading.Morning -> Triple(s.morningAzkar, s.book, data.morning)
        Reading.Evening -> Triple(s.eveningAzkar, s.book, data.evening)
        is Reading.OfChapter -> data.chapterById.getValue(reading.id).let {
            val source = when {
                it.items.isNotEmpty() && it.items.all { z -> z.ref?.en?.startsWith("Quran") == true } -> s.fromQuran
                it.items.any { z -> z.ref != null } -> s.fromSunnah
                else -> s.book
            }
            Triple(it.title(lang), if (lang == Lang.AR) source else it.ar, it.items)
        }
    }
    val remaining = remember(reading) { mutableStateMapOf<Int, Int>() }
    fun left(z: Zikr) = remaining[z.id] ?: z.repeat
    val totalReps = items.sumOf { it.repeat }
    val doneReps = items.sumOf { it.repeat - left(it) }
    val allDone = doneReps == totalReps
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    CollapsingTitle(title, listState)

    LaunchedEffect(focus) {
        val idx = items.indexOfFirst { it.id == focus }
        if (idx >= 0) listState.scrollToItem(idx + 1)
    }

    // Finishing the last dhikr opens the congratulation card (streaks count for morning / evening).
    LaunchedEffect(allDone) {
        if (allDone && totalReps > 0 && remaining.isNotEmpty()) {
            delay(450)
            com.koussa.islamicazkar.data.AzkarHistory.markReading(ctx, reading.key())   // the Record tab
            // From the record itself (the Record tab), which also holds the other devices' days.
            val streak = maxOf(com.koussa.islamicazkar.data.AzkarHistory.streak(ctx, reading.key()), 1)
            app.celebration = Celebration(
                reading = reading, title = title, azkar = items.size, repetitions = totalReps,
                streak = streak, encouragement = (System.currentTimeMillis() / 86_400_000L).toInt() + items.size,
            )
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(), state = listState,
        contentPadding = screenPadding(hasBack = true), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                LargeTitle(title, "$subtitle · ${s.azkar(items.size)}")
                ProgressBar(doneReps / totalReps.toFloat())
            }
        }
        itemsIndexed(items, key = { _, z -> z.id }) { i, z ->
            fun set(r: Int) {
                remaining[z.id] = r
                if (r == 0) {
                    view.tick(app.settings, strong = true)
                    if (i + 1 < items.size) scope.launch { delay(280); listState.animateScrollToItem(i + 2) }
                }
            }
            ZikrCard(
                z = z, index = i + 1, total = items.size, remaining = left(z), chapterTitle = title,
                onCount = { set(left(z) - 1) },
                onDone = { set(0) },
            )
        }
        item {
            AnimatedVisibility(allDone, enter = fadeIn() + scaleIn(initialScale = 0.95f)) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = g.accent, modifier = Modifier.size(44.dp))
                        Text(
                            s.allDone, color = g.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                        AccentButton(s.restart, icon = Icons.Rounded.Restore) {
                            remaining.clear(); scope.launch { listState.animateScrollToItem(0) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(p: Float) {
    val g = Glass.colors
    val v by animateFloatAsState(p, label = "progress")
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp).height(4.dp).clip(RoundedCornerShape(50)).background(g.fillGray).drawBehind {
            val w = size.width * v
            val left = if (layoutDirection == LayoutDirection.Rtl) size.width - w else 0f
            drawRect(g.accent, topLeft = Offset(left, 0f), size = Size(w, size.height))
        },
    )
}

@Composable
fun FavoritesScreen() {
    val app = LocalApp.current
    val ctx = LocalContext.current
    val data = remember { AzkarRepository.get(ctx) }
    val g = Glass.colors
    val s = Glass.s
    val lang = Glass.lang
    val favs = app.settings.favorites.mapNotNull { data.zikrById[it] }.sortedBy { it.id % 10000 }
    val remaining = remember { mutableStateMapOf<Int, Int>() }
    val listState = rememberLazyListState()
    CollapsingTitle(s.tabFavorites, listState)

    fun chapterTitle(z: Zikr): String = when (val c = data.chapterOfZikr[z.id]) {
        -1 -> s.morningAzkar
        -2 -> s.eveningAzkar
        null -> s.book
        else -> data.chapterById.getValue(c).title(lang)
    }

    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = screenPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LargeTitle(s.tabFavorites, if (favs.isEmpty()) null else s.azkar(favs.size)) }
        if (favs.isEmpty()) {
            item {
                // ContentUnavailableView-style empty state.
                Column(Modifier.fillMaxWidth().padding(top = 120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.FavoriteBorder, null, tint = g.text3, modifier = Modifier.size(52.dp))
                    Text(s.noFavorites, color = g.text, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(top = 12.dp))
                    Text(s.noFavoritesHint, color = g.text2, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp))
                }
            }
        }
        items(favs, key = { it.id }) { z ->
            ZikrCard(
                z = z, index = null, total = null, remaining = remaining[z.id] ?: z.repeat,
                chapterTitle = chapterTitle(z), label = chapterTitle(z),
                onCount = { remaining[z.id] = (remaining[z.id] ?: z.repeat) - 1 },
                onDone = { remaining[z.id] = 0 },
            )
        }
    }
}

