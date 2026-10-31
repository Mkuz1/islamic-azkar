package com.koussa.islamicazkar.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.TasbihPhrases
import com.koussa.islamicazkar.i18n.num
import com.koussa.islamicazkar.ui.glass.ArabicFont
import com.koussa.islamicazkar.ui.glass.Glass
import com.koussa.islamicazkar.ui.glass.GlassIconButton
import com.koussa.islamicazkar.ui.glass.GlassSegmented
import com.koussa.islamicazkar.ui.glass.ProgressRing
import com.koussa.islamicazkar.ui.glass.glass
import com.koussa.islamicazkar.ui.glass.pane
import com.koussa.islamicazkar.ui.glass.pressable
import com.koussa.islamicazkar.widget.Widgets
import kotlinx.coroutines.launch

private val TARGETS = listOf(33, 99, 100, 0)

@Composable
fun TasbihScreen() {
    val app = LocalApp.current
    val st = app.settings
    val ctx = LocalContext.current
    val view = LocalView.current
    val g = Glass.colors
    val s = Glass.s
    val lang = Glass.lang
    val pulse = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val count = st.tasbihCount
    val target = st.tasbihTarget
    val inRound = if (target > 0) count % target else count
    val rounds = if (target > 0) count / target else 0

    fun increment() {
        val c = count + 1
        val roundDone = target > 0 && c % target == 0
        view.tick(st, strong = roundDone)
        st.setTasbih(count = c, total = st.tasbihTotal + 1)
        com.koussa.islamicazkar.data.TasbihHistory.add(ctx)
        Widgets.refreshTasbih(ctx)
        scope.launch { pulse.snapTo(1.035f); pulse.animateTo(1f, tween(260)) }
    }

    val hinge = LocalHinge.current
    val tabletop = hinge?.takeIf { !it.vertical && it.halfOpened }

    val orb: @Composable (Modifier) -> Unit = { m ->
        Box(m.scale(pulse.value).pressable(::increment), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .pane(CircleShape),
            )
            ProgressRing(if (target > 0) inRound / target.toFloat() else 0f, Modifier.fillMaxSize().padding(10.dp), stroke = 8.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(TasbihPhrases.ar[st.tasbihPhrase], fontFamily = ArabicFont, fontSize = 20.sp, lineHeight = 30.sp, color = g.text2, textAlign = TextAlign.Center)
                AnimatedContent(
                    targetState = inRound,
                    transitionSpec = { (slideInVertically { -it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut()) },
                    label = "n",
                ) { n ->
                    // Arabic-Indic digits in a Naskh face read unmistakably as Arabic (a sans ٧ looks like "V").
                    if (lang == Lang.AR) Text(
                        lang.num(n), fontFamily = ArabicFont, fontWeight = FontWeight.Bold, fontSize = 70.sp, lineHeight = 72.sp, color = g.text,
                        style = androidx.compose.ui.text.TextStyle(
                            lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                                androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                                androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
                            ),
                        ),
                    )
                    else Text(lang.num(n), fontSize = 72.sp, fontWeight = FontWeight.SemiBold, color = g.text, letterSpacing = (-1).sp)
                }
                // Words rather than "/" and "×", so right-to-left text never reorders the numbers.
                Text(if (target > 0) s.ofTarget(lang.num(target)) else "∞", color = g.text2, fontSize = 15.sp)

            }
        }
    }

    val phrases: @Composable () -> Unit = {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(TasbihPhrases.ar) { i, phrase ->
                val sel = i == st.tasbihPhrase
                Text(
                    phrase, fontFamily = ArabicFont, fontSize = 17.sp,
                    color = if (sel) g.onAccent else g.text,
                    modifier = Modifier
                        .pressable({ st.setTasbih(phrase = i, count = 0); Widgets.refreshTasbih(ctx) })
                        .clip(RoundedCornerShape(50))
                        .background(if (sel) g.accent else g.fillGray)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }

    val controls: @Composable () -> Unit = {
        if (rounds > 0) {
            Text(
                s.roundsDone(lang.num(rounds)), color = g.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
        val meaning = TasbihPhrases.meaning(st.tasbihPhrase, lang)
        if (meaning.isNotEmpty()) {
            Text(meaning, color = g.text2, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        }
        Text(s.tapAnywhere, color = g.text3, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f)) {
                GlassSegmented(
                    TARGETS.map { if (it == 0) "∞" else lang.num(it) },
                    TARGETS.indexOf(target).coerceAtLeast(0),
                ) { st.setTasbih(target = TARGETS[it], count = 0); Widgets.refreshTasbih(ctx) }
            }
            Spacer(Modifier.size(12.dp))
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(g.fillGray)
                    .pressable({ view.tick(st, strong = true); st.setTasbih(count = 0); Widgets.refreshTasbih(ctx) }),
                contentAlignment = Alignment.Center,
            ) { androidx.compose.material3.Icon(Icons.Rounded.Restore, s.reset, tint = g.accent, modifier = Modifier.size(22.dp)) }
        }
        Text(s.target, color = g.text3, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 12.dp))
    }

    if (tabletop != null) {
        // Tabletop posture: the bead fills the upper half, controls rest on the lower half.
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(tabletop.start).statusBarsPadding(), contentAlignment = Alignment.Center) {
                orb(Modifier.fillMaxHeight(0.92f).aspectRatio(1f))
            }
            Spacer(Modifier.height(tabletop.end - tabletop.start))
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp).padding(bottom = 96.dp)) {
                phrases()
                controls()
            }
        }
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(screenPadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LargeTitle(s.tasbih, "${s.total}: ${lang.num(st.tasbihTotal)}", Modifier.fillMaxWidth())
        phrases()
        Spacer(Modifier.height(26.dp))
        orb(Modifier.widthIn(max = 300.dp).fillMaxWidth(0.82f).aspectRatio(1f))
        controls()
    }
}
