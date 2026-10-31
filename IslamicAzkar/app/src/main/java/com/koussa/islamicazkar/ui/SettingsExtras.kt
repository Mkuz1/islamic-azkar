package com.koussa.islamicazkar.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.koussa.islamicazkar.data.CalcMethod
import com.koussa.islamicazkar.data.Cities
import com.koussa.islamicazkar.data.Hijri
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.Locator
import com.koussa.islamicazkar.data.Palette
import com.koussa.islamicazkar.data.Place
import com.koussa.islamicazkar.data.Prayers
import com.koussa.islamicazkar.data.Script
import com.koussa.islamicazkar.i18n.MUSHAF_URL
import com.koussa.islamicazkar.i18n.more
import com.koussa.islamicazkar.ui.glass.CalligraphyFont
import com.koussa.islamicazkar.ui.glass.Glass
import com.koussa.islamicazkar.ui.glass.pressable
import com.koussa.islamicazkar.widget.MoreWidgets
import com.koussa.islamicazkar.widget.Widgets
import com.koussa.islamicazkar.widget.prayerClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Prayer times: location, calculation method, Asr school and today's timetable. */
@Composable
fun PrayerSettings() {
    val app = LocalApp.current
    val st = app.settings
    val ctx = LocalContext.current
    val g = Glass.colors
    val lang = Glass.lang
    val m = more(lang)
    var placeDialog by remember { mutableStateOf(false) }
    var methodDialog by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun changed() = MoreWidgets.refreshAll(ctx)
    fun locate() {
        locating = true
        scope.launch {
            val p = runCatching { Locator.find(ctx) }.getOrNull()
            locating = false
            if (p != null) { st.updatePlace(p); placeDialog = false; changed() } else app.toast = m.locationFailed
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) locate() else app.toast = m.locationFailed
    }

    Column {
        SectionHeader(m.prayerTimes)
        InsetGroup {
            ListRow(
                m.location, icon = Icons.Rounded.LocationOn, iconColor = Color(0xFF34C759),
                subtitle = st.place?.label(lang)?.ifBlank { m.myLocation } ?: m.notSet, chevron = true, onClick = { placeDialog = true },
            )
            RowDivider()
            ListRow(m.method, icon = Icons.Rounded.Calculate, iconColor = Color(0xFF5856D6), subtitle = m.methods[st.method], chevron = true, onClick = { methodDialog = true })
            RowDivider()
            ListRow(m.asrHanafi, icon = Icons.Rounded.WbTwilight, iconColor = Color(0xFFFF9500), trailing = {
                com.koussa.islamicazkar.ui.glass.IosSwitch(st.asrHanafi, onChange = { st.updateAsrHanafi(it); changed() })
            })
            st.place?.let { place -> RowDivider(); Timetable(ctx, place) }
        }
        SectionFooter(m.asrHint)
    }

    if (placeDialog) {
        SheetDialog(onDismiss = { placeDialog = false }) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(g.accent.copy(alpha = 0.14f))
                    .pressable(onClick = {
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) locate()
                        else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }, enabled = !locating)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.MyLocation, null, tint = g.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(if (locating) m.locating else m.useMyLocation, color = g.accent, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(m.chooseCity, color = g.text2, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp, start = 4.dp))
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                itemsIndexed(Cities.all) { i, city ->
                    if (i > 0) Box(Modifier.fillMaxWidth().padding(start = 4.dp).height(0.5.dp).background(g.separator))
                    PickRow(city.label(lang), st.place?.let { it.lat == city.lat && it.lng == city.lng } == true) {
                        st.updatePlace(city); placeDialog = false; changed()
                    }
                }
            }
        }
    }
    if (methodDialog) {
        SheetDialog(onDismiss = { methodDialog = false }) {
            CalcMethod.entries.forEachIndexed { i, _ ->
                PickRow(m.methods[i], st.method == i) { st.updateMethod(i); methodDialog = false; changed() }
            }
        }
    }
}

/** Today's six times with the next prayer highlighted, refreshed every minute. */
@Composable
private fun Timetable(ctx: Context, place: Place) {
    val g = Glass.colors
    val lang = Glass.lang
    val m = more(lang)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    val st = LocalApp.current.settings
    val times = remember(place, now / 60_000, st.method, st.asrHanafi) { Prayers.times(ctx, place, now) }
    val (next, at) = remember(times) { Prayers.next(ctx, place, now) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        for (i in 0 until 6) {
            val on = i == next && at <= times[5]
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (on) g.accent.copy(alpha = 0.14f) else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(more(Lang.AR).prayers[i], fontFamily = CalligraphyFont, fontSize = 20.sp, color = if (on) g.accent else g.text)
                if (lang != Lang.AR) Text("  " + m.prayers[i], fontSize = 14.sp, color = g.text2)
                Spacer(Modifier.weight(1f))
                Text(prayerClock(lang, times[i], place.tz), fontSize = 17.sp, color = if (i == 1) g.text2 else g.text, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

/** Widget colours, calligraphy, Hijri adjustment and the lock-screen glance. */
@Composable
fun WidgetStyleSettings(withNotifications: (() -> Unit) -> Unit) {
    val app = LocalApp.current
    val st = app.settings
    val ctx = LocalContext.current
    val g = Glass.colors
    val lang = Glass.lang
    val m = more(lang)
    fun restyle() = Widgets.refreshEverything(ctx)

    Column {
        SectionHeader(m.widgetStyle)
        InsetGroup {
            Column(Modifier.padding(16.dp)) {
                Text(m.palette, color = g.text, fontSize = 17.sp)
                Row(Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Palette.entries.forEach { p ->
                        val on = st.palette == p.ordinal
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .drawBehind { drawRect(Brush.verticalGradient(listOf(Color(p.top), Color(p.bottom)))) }
                                .border(if (on) 3.dp else 1.dp, if (on) Color(0xFFD6B36A) else Color.White.copy(alpha = 0.35f), CircleShape)
                                .pressable(onClick = { st.updatePalette(p.ordinal); restyle() }),
                        )
                    }
                }
                Text(Palette.entries[st.palette].label(lang), color = g.text2, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
            RowDivider()
            Column(Modifier.padding(16.dp)) {
                Text(m.calligraphy, color = g.text, fontSize = 17.sp)
                Row(Modifier.padding(top = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Script.entries.forEach { sc ->
                        val on = st.script == sc.ordinal
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                .background(if (on) g.accent.copy(alpha = 0.16f) else g.fillGray)
                                .border(if (on) 1.5.dp else 0.dp, if (on) g.accent else Color.Transparent, RoundedCornerShape(14.dp))
                                .pressable(onClick = { st.updateScript(sc.ordinal); restyle() })
                                .height(80.dp)
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            Text(
                                "الجمعة", fontFamily = remember(sc) { FontFamily(Font(sc.font)) },
                                fontWeight = if (sc.bold) FontWeight.Bold else FontWeight.Normal,
                                fontSize = if (sc == Script.NASTALIQ) 17.sp else 22.sp, color = if (on) g.accent else g.text, maxLines = 1,
                            )
                            Text(sc.label(lang), fontSize = 11.sp, color = g.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            RowDivider()
            Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SymbolSquare(Icons.Rounded.CalendarMonth, Color(0xFFFF3B30))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.hijriOffset, color = g.text, fontSize = 17.sp)
                    Text(Hijri.format(lang, Hijri.of(System.currentTimeMillis(), st.hijriOffset)) + " · " + m.days(st.hijriOffset), color = g.text2, fontSize = 13.sp)
                }
                Stepper(onMinus = { if (st.hijriOffset > -2) { st.updateHijriOffset(st.hijriOffset - 1); restyle() } }, onPlus = { if (st.hijriOffset < 2) { st.updateHijriOffset(st.hijriOffset + 1); restyle() } })
            }
            RowDivider()
            ListRow(m.lockScreenOn, icon = Icons.Rounded.Lock, iconColor = Color(0xFF8E8E93), trailing = {
                com.koussa.islamicazkar.ui.glass.IosSwitch(st.lockGlance, onChange = { on ->
                    if (on) withNotifications { st.updateLockGlance(true); MoreWidgets.refreshAll(ctx) }
                    else { st.updateLockGlance(false); MoreWidgets.refreshAll(ctx) }
                })
            })
        }
        SectionFooter(m.hijriOffsetHint + "\n\n" + m.lockScreenHint)
    }
}

/** An invitation to install our Mushaf app. */
@Composable
fun OurApps() {
    val ctx = LocalContext.current
    val g = Glass.colors
    val m = more(Glass.lang)
    Column {
        SectionHeader(m.ourApps)
        InsetGroup {
            Row(
                Modifier.fillMaxWidth().pressable(onClick = {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MUSHAF_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(60.dp).clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF26473C), Color(0xFF0A1C15))))
                        .border(1.dp, Color(0x66F1DBA3), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("مصحف", fontFamily = CalligraphyFont, color = Color(0xFFF1DBA3), fontSize = 19.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.mushafTitle, color = g.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(m.mushafText, color = g.text2, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
                    Text(m.mushafCta, color = g.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun Stepper(onMinus: () -> Unit, onPlus: () -> Unit) {
    val g = Glass.colors
    Row(Modifier.clip(RoundedCornerShape(9.dp)).background(g.fillGray), verticalAlignment = Alignment.CenterVertically) {
        Text("−", color = g.text, fontSize = 20.sp, modifier = Modifier.pressable(onMinus).padding(horizontal = 14.dp, vertical = 3.dp))
        Box(Modifier.size(width = 1.dp, height = 18.dp).background(g.separator))
        Text("+", color = g.text, fontSize = 20.sp, modifier = Modifier.pressable(onPlus).padding(horizontal = 14.dp, vertical = 3.dp))
    }
}

@Composable
private fun PickRow(title: String, selected: Boolean, onClick: () -> Unit) {
    val g = Glass.colors
    Row(Modifier.fillMaxWidth().pressable(onClick).padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = g.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Rounded.Check, null, tint = g.accent, modifier = Modifier.size(20.dp))
    }
}

/** A glass dialog card, like the time picker's. */
@Composable
private fun SheetDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val g = Glass.colors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .drawBehind {
                    drawRect(if (g.dark) Color(0xFF14211F) else Color(0xFFF7FBFA))
                    drawRect(Brush.verticalGradient(0f to g.highlight, 0.4f to Color.Transparent))
                }
                .border(1.dp, Brush.verticalGradient(listOf(g.rimLight, g.rimDark)), RoundedCornerShape(28.dp))
                .padding(18.dp),
        ) { content() }
    }
}
