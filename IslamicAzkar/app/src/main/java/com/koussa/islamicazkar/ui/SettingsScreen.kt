package com.koussa.islamicazkar.ui

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.data.ThemeMode
import com.koussa.islamicazkar.i18n.clock
import com.koussa.islamicazkar.i18n.num
import com.koussa.islamicazkar.notify.Reminders
import com.koussa.islamicazkar.ui.glass.ArabicFont
import com.koussa.islamicazkar.ui.glass.Glass
import com.koussa.islamicazkar.ui.glass.GlassSegmented
import com.koussa.islamicazkar.ui.glass.Hairline
import com.koussa.islamicazkar.ui.glass.IosSwitch
import com.koussa.islamicazkar.ui.glass.pressable
import com.koussa.islamicazkar.widget.DhikrWidget
import com.koussa.islamicazkar.widget.QuickWidget
import com.koussa.islamicazkar.widget.TasbihWidget

private val EVERY = listOf(1, 2, 3, 4, 6)

@Composable
fun SettingsScreen() {
    val app = LocalApp.current
    val st = app.settings
    val ctx = LocalContext.current
    val g = Glass.colors
    val s = Glass.s
    val lang = Glass.lang
    val m = com.koussa.islamicazkar.i18n.more(lang)
    var picking by remember { mutableStateOf<Pair<Int, (Int) -> Unit>?>(null) }

    // Runs [action] once notifications are allowed (asking on Android 13+).
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pending?.invoke() else app.toast = s.permissionDenied
        pending = null
    }
    fun withNotifications(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pending = { action(); Reminders.reschedule(ctx) }
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action(); Reminders.reschedule(ctx)
        }
    }
    fun apply(action: () -> Unit) { action(); Reminders.reschedule(ctx) }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    CollapsingTitle(s.settings, listState)
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = screenPadding()) {
        item { LargeTitle(s.settings) }

        item { SectionHeader(s.language) }
        item {
            InsetGroup {
                Box(Modifier.padding(12.dp)) {
                    GlassSegmented(listOf("العربية", "English", "Français"), Lang.entries.indexOf(st.lang)) {
                        st.updateLang(Lang.entries[it]); Reminders.reschedule(ctx); com.koussa.islamicazkar.widget.Widgets.refreshEverything(ctx)
                    }
                }
            }
        }

        item { SectionHeader(s.appearance) }
        item {
            InsetGroup {
                Box(Modifier.padding(12.dp)) {
                    GlassSegmented(listOf(s.themeSystem, s.themeLight, s.themeDark), st.theme.ordinal) { st.updateTheme(ThemeMode.entries[it]) }
                }
            }
        }

        item { SectionHeader(s.reading) }
        item {
            InsetGroup {
                ListRow(s.textSize, icon = Icons.Rounded.FormatSize, iconColor = Color(0xFF007AFF), value = lang.num((st.textScale * 100).toInt()) + "%")
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    Slider(
                        st.textScale, { st.updateTextScale(it) }, valueRange = 0.8f..1.6f, steps = 7,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = g.accent, inactiveTrackColor = g.fillGray, activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent),
                    )
                    Text(
                        "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", fontFamily = ArabicFont, fontSize = (23 * st.textScale).sp,
                        color = g.text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                }
                RowDivider()
                ToggleRow(Icons.Rounded.TextFields, Color(0xFF34C759), s.showTranslit, st.showTranslit) { st.updateShowTranslit(it) }
                RowDivider()
                ToggleRow(Icons.Rounded.Translate, Color(0xFF5856D6), s.showMeaning, st.showMeaning) { st.updateShowMeaning(it) }
                RowDivider()
                ToggleRow(Icons.Rounded.Vibration, Color(0xFFFF2D55), s.haptics, st.haptics) { st.updateHaptics(it) }
            }
        }

        item { SectionHeader(s.notifications) }
        item {
            InsetGroup {
                ToggleRow(Icons.Rounded.WbSunny, Color(0xFFFF9500), s.morningReminder, st.morningOn) { on ->
                    if (on) withNotifications { st.setMorning(on = true) } else apply { st.setMorning(on = false) }
                }
                if (st.morningOn) { RowDivider(); TimeRow(s.time, st.morningAt) { picking = st.morningAt to { m -> apply { st.setMorning(at = m) } } } }
                RowDivider()
                ToggleRow(Icons.Rounded.NightsStay, Color(0xFF5856D6), s.eveningReminder, st.eveningOn) { on ->
                    if (on) withNotifications { st.setEvening(on = true) } else apply { st.setEvening(on = false) }
                }
                if (st.eveningOn) { RowDivider(); TimeRow(s.time, st.eveningAt) { picking = st.eveningAt to { m -> apply { st.setEvening(at = m) } } } }
                RowDivider()
                ToggleRow(Icons.Rounded.NotificationsActive, Color(0xFFFF3B30), s.generalReminders, st.generalOn) { on ->
                    if (on) withNotifications { st.setGeneral(on = true) } else apply { st.setGeneral(on = false) }
                }
                if (st.generalOn) {
                    RowDivider()
                    Column(Modifier.padding(start = 58.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)) {
                        Text("${s.every} …", color = g.text2, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                        GlassSegmented(EVERY.map { lang.num(it) + if (lang == Lang.AR) " س" else " h" }, EVERY.indexOf(st.generalEveryHours).coerceAtLeast(0)) {
                            apply { st.setGeneral(everyHours = EVERY[it]) }
                        }
                    }
                    RowDivider()
                    TimeRow(s.between, st.generalFrom) { picking = st.generalFrom to { m -> apply { st.setGeneral(from = m) } } }
                    RowDivider()
                    TimeRow(s.and, st.generalTo) { picking = st.generalTo to { m -> apply { st.setGeneral(to = m) } } }
                }
            }
        }
        item { SectionFooter(s.generalRemindersHint) }

        item { PrayerSettings() }


        item { OurApps() }

        item { SectionHeader(s.about) }
        item {
            InsetGroup {
                ListRow(s.appName, icon = Icons.Rounded.Info, iconColor = Color(0xFF8E8E93), value = "1.1")
                RowDivider()
                Column(Modifier.padding(16.dp)) {
                    Text(s.aboutText, color = g.text, fontSize = 15.sp, lineHeight = 21.sp)
                    if (lang != Lang.EN) {
                        Text(s.translationNote, color = g.text2, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
        item {
            Text(
                DEDICATION_AR, fontFamily = ArabicFont, color = g.text3, fontSize = 15.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
        }
    }

    picking?.let { (initial, onPick) ->
        TimeDialog(initial, onDismiss = { picking = null }) { onPick(it); picking = null }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, hue: Color, title: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    val g = Glass.colors
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolSquare(icon, hue)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = g.text, fontSize = 17.sp)
            if (subtitle != null) Text(subtitle, color = g.text2, fontSize = 13.sp)
        }
        IosSwitch(checked, onChange)
    }
}

@Composable
private fun TimeRow(label: String, minutes: Int, onClick: () -> Unit) {
    val g = Glass.colors
    Row(
        Modifier.fillMaxWidth().pressable(onClick).defaultMinSize(minHeight = 48.dp).padding(start = 58.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = g.text, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Text(
            Glass.lang.clock(minutes), color = g.text, fontSize = 17.sp,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(g.fillGray).padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val g = Glass.colors
    val s = Glass.s
    val state = rememberTimePickerState(initial / 60, initial % 60, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(32.dp))
                .drawBehind {
                    drawRect(if (g.dark) Color(0xFF14211F) else Color(0xFFF7FBFA))
                    drawRect(Brush.verticalGradient(0f to g.highlight, 0.4f to Color.Transparent))
                }
                .border(1.dp, Brush.verticalGradient(listOf(g.rimLight, g.rimDark)), RoundedCornerShape(32.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimePicker(
                state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = g.separator, selectorColor = g.accent,
                    timeSelectorSelectedContainerColor = g.accentSoft, timeSelectorSelectedContentColor = g.accent,
                    timeSelectorUnselectedContainerColor = g.separator, timeSelectorUnselectedContentColor = g.text,
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onDismiss) { Text(s.cancel, color = g.text2) }
                TextButton({ onPick(state.hour * 60 + state.minute) }) { Text(s.ok, color = g.accent, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
