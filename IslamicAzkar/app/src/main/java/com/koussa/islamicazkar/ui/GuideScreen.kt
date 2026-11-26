package com.koussa.islamicazkar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koussa.islamicazkar.data.AzkarRepository
import com.koussa.islamicazkar.data.Guide
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Reading
import com.koussa.islamicazkar.i18n.num
import com.koussa.islamicazkar.ui.glass.ArabicFont
import com.koussa.islamicazkar.ui.glass.Glass
import com.koussa.islamicazkar.ui.glass.pressable

/** Row that opens the ʿUmrah guide; shown on Home and in the Hajj & Umrah category. */
@Composable
fun GuideRow(guide: Guide) {
    val app = LocalApp.current
    ListRow(
        title = guide.title[Glass.lang], subtitle = Glass.s.guideCta,
        icon = Icons.Rounded.Explore, iconColor = Color(0xFFA2845E), chevron = true,
        onClick = { app.push(Route.GuideRoute(guide.key)) },
    )
}

/**
 * The step-by-step guide, in the reader's card system: each step is a numbered card (title,
 * explanation, sources) followed by its duas as full [ZikrCard]s with counters, favourite,
 * copy and share. A progress bar counts the guide's duas like any reading.
 */
@Composable
fun GuideScreen(key: String) {
    val ctx = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val app = LocalApp.current
    val data = remember { AzkarRepository.get(ctx) }
    val g = Glass.colors
    val s = Glass.s
    val lang = Glass.lang
    val guide = data.guides.firstOrNull { it.key == key } ?: return
    val listState = rememberLazyListState()
    CollapsingTitle(guide.title[lang], listState)

    val steps = remember(guide) { guide.steps.map { st -> st to st.zikr.mapNotNull { data.zikrById[it] } } }
    val all = remember(steps) { steps.flatMap { it.second } }
    val remaining = remember(key) { androidx.compose.runtime.mutableStateMapOf<Int, Int>() }
    fun left(z: com.koussa.islamicazkar.data.Zikr) = remaining[z.id] ?: z.repeat
    val total = all.sumOf { it.repeat }
    val done = all.sumOf { it.repeat - left(it) }
    val progress by androidx.compose.animation.core.animateFloatAsState(if (total == 0) 0f else done / total.toFloat(), label = "guide")

    LazyColumn(
        Modifier.fillMaxSize(), state = listState,
        contentPadding = screenPadding(hasBack = true), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                LargeTitle(guide.title[lang], guide.intro[lang])
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress }, color = g.accent, trackColor = g.fillGray, drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(4.dp),
                )
            }
        }
        var index = 0
        steps.forEachIndexed { si, (step, duas) ->
            item(key = "step$si") { StepCard(si + 1, guide.steps.size, step) }
            duas.forEach { z ->
                val number = ++index
                item(key = "z${z.id}") {
                    ZikrCard(
                        z = z, index = number, total = all.size, remaining = left(z), chapterTitle = guide.title[lang],
                        onCount = { remaining[z.id] = left(z) - 1; if (left(z) == 0) view.tick(app.settings, strong = true) },
                        onDone = { remaining[z.id] = 0 },
                    )
                }
            }
            item(key = "gap$si") { Spacer(Modifier.height(10.dp)) }
        }
        item {
            Text(
                guide.note[lang], color = g.text3, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/** A numbered step: gold number badge, "3 of 9", title, explanation and hadith sources. */
@Composable
private fun StepCard(number: Int, count: Int, step: com.koussa.islamicazkar.data.GuideStep) {
    val g = Glass.colors
    val lang = Glass.lang
    val gold = Color(0xFFB8913F)
    Card(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(
                Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape).background(gold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { Text(lang.num(number), color = gold, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
            Spacer(Modifier.width(10.dp))
            Capsule("${lang.num(number)} ${Glass.s.ofTarget(lang.num(count))}")
        }
        Text(
            step.title[lang].replace(Regex("^[0-9٠-٩]+\\.\\s*"), ""),
            color = g.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp),
        )
        Text(step.text[lang], color = g.text2, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.padding(top = 8.dp))
        if (step.ref[lang].isNotBlank()) {
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.MenuBook, null, tint = g.text3, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(step.ref[lang], color = g.text3, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
