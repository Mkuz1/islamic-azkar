package com.koussa.islamicazkar.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/** The step-by-step guide: each step's explanation, the duas to say there, and its hadith sources. */
@Composable
fun GuideScreen(key: String) {
    val app = LocalApp.current
    val ctx = LocalContext.current
    val data = remember { AzkarRepository.get(ctx) }
    val g = Glass.colors
    val s = Glass.s
    val lang = Glass.lang
    val guide = data.guides.firstOrNull { it.key == key } ?: return
    val listState = rememberLazyListState()
    CollapsingTitle(guide.title[lang], listState)

    LazyColumn(
        Modifier.fillMaxSize(), state = listState,
        contentPadding = screenPadding(hasBack = true), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { LargeTitle(guide.title[lang], guide.intro[lang]) }
        items(guide.steps, key = { it.title.en }) { step ->
            Card(Modifier.fillMaxWidth()) {
                Text(step.title[lang], color = g.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Text(step.text[lang], color = g.text2, fontSize = 16.sp, lineHeight = 23.sp, modifier = Modifier.padding(top = 8.dp))
                val duas = step.zikr.mapNotNull { id -> data.zikrById[id]?.let { it to data.chapterOfZikr[id] } }
                if (duas.isNotEmpty()) {
                    SectionHeader(s.wordsToSay, Modifier.padding(top = 10.dp))
                    InsetGroup {
                        duas.forEachIndexed { i, (z, chapter) ->
                            if (i > 0) RowDivider(16.dp)
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                                    .then(if (chapter != null && chapter > 0) Modifier.pressable(onClick = { app.push(Route.Reader(Reading.OfChapter(chapter), z.id)) }) else Modifier),
                            ) {
                                Text(
                                    z.ar, fontFamily = ArabicFont, fontSize = 19.sp, lineHeight = 32.sp, color = g.text,
                                    textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(),
                                )
                                val m = z.meaning(lang)
                                if (m.isNotBlank()) Text(m, color = g.text2, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if (step.ref[lang].isNotBlank()) {
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.MenuBook, null, tint = g.text3, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(step.ref[lang], color = g.text3, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            Text(
                guide.note[lang], color = g.text3, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
