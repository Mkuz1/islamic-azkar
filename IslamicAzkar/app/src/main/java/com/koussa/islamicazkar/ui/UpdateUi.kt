package com.koussa.islamicazkar.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koussa.islamicazkar.BuildConfig
import com.koussa.islamicazkar.ui.glass.AccentButton
import com.koussa.islamicazkar.ui.glass.Glass
import com.koussa.islamicazkar.ui.glass.pane
import com.koussa.islamicazkar.ui.glass.pressable
import com.koussa.islamicazkar.update.Release
import com.koussa.islamicazkar.update.UpdateBus
import com.koussa.islamicazkar.update.UpdateState
import com.koussa.islamicazkar.update.appUpdater
import com.koussa.islamicazkar.update.updateTexts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val PREFS = "updates"
private const val CHECK_EVERY_MS = 12 * 60 * 60 * 1000L

private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

/** Checks GitHub for a newer build; [manual] ignores the 12-hour throttle and a dismissed version. */
fun checkForUpdate(ctx: Context, scope: CoroutineScope, manual: Boolean) {
    val updater = appUpdater()
    if (!updater.supported) return
    val p = prefs(ctx)
    val now = System.currentTimeMillis()
    if (!manual && now - p.getLong("lastCheck", 0) < CHECK_EVERY_MS) return
    if (UpdateBus.state.value is UpdateState.Downloading || UpdateBus.state.value is UpdateState.Installing) return
    if (manual) UpdateBus.state.value = UpdateState.Checking
    scope.launch {
        val result = runCatching { updater.newerRelease(ctx) }
        p.edit().putLong("lastCheck", now).apply()
        val r = result.getOrNull()
        UpdateBus.state.value = when {
            result.isFailure -> if (manual) UpdateState.Failed(result.exceptionOrNull()?.message) else UpdateState.Idle
            r == null -> if (manual) UpdateState.UpToDate else UpdateState.Idle
            !manual && p.getString("dismissed", null) == r.tag -> UpdateState.Idle
            else -> UpdateState.Available(r)
        }
    }
}

fun startUpdate(ctx: Context, scope: CoroutineScope, release: Release, lang: com.koussa.islamicazkar.data.Lang) {
    UpdateBus.state.value = UpdateState.Downloading(release, 0f)
    scope.launch {
        runCatching {
            appUpdater().downloadAndInstall(ctx, release) { UpdateBus.state.value = UpdateState.Downloading(release, it) }
        }.onSuccess {
            UpdateBus.state.value = UpdateState.Installing(release)
        }.onFailure { e ->
            UpdateBus.state.value = if (e.javaClass.simpleName == "NeedsPermission") UpdateState.Failed(updateTexts(lang).allowInstall)
            else UpdateState.Failed(null)
        }
    }
}

/** Floating card announcing a new version (github build only); checks once at launch. */
@Composable
fun UpdateHost(modifier: Modifier = Modifier) {
    if (!appUpdater().supported) return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by UpdateBus.current.collectAsState()
    LaunchedEffect(Unit) { checkForUpdate(ctx, scope, manual = false) }
    val g = Glass.colors
    val t = updateTexts(Glass.lang)
    var shownFor by remember { mutableStateOf<Release?>(null) }
    val release = when (val s = state) {
        is UpdateState.Available -> s.release
        is UpdateState.Downloading -> s.release
        is UpdateState.Installing -> s.release
        else -> null
    }
    if (release != null) shownFor = release
    val visible = release != null

    AnimatedVisibility(visible, modifier = modifier, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
        val r = shownFor ?: return@AnimatedVisibility
        Column(
            Modifier.padding(horizontal = 16.dp).widthIn(max = 520.dp).fillMaxWidth()
                .pane(RoundedCornerShape(26.dp)).padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SymbolSquare(Icons.Rounded.SystemUpdate, Color(0xFF34C759), 36.dp)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(t.available, color = g.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(t.version(r.version), color = g.text2, fontSize = 14.sp)
                }
            }
            if (r.notes.isNotBlank()) {
                Text(r.notes.trim(), color = g.text2, fontSize = 14.sp, lineHeight = 19.sp, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
            }
            when (val s = state) {
                is UpdateState.Downloading -> UpdateProgress(t.downloading, s.progress)
                is UpdateState.Installing -> UpdateProgress(t.installing, 1f)
                else -> Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        t.later, color = g.accent, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.pressable(onClick = {
                            prefs(ctx).edit().putString("dismissed", r.tag).apply()
                            UpdateBus.state.value = UpdateState.Idle
                        }).padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        AccentButton(t.install, icon = Icons.Rounded.SystemUpdate) { startUpdate(ctx, scope, r, Glass.lang) }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateProgress(label: String, p: Float) {
    val g = Glass.colors
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Text(label, color = g.text2, fontSize = 14.sp)
        Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(g.fillGray)) {
            Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(50)).background(g.accent))
        }
    }
}

/** Settings row: installed version and a manual check (github build only). */
@Composable
fun UpdateSettingsRow() {
    if (!appUpdater().supported) return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by UpdateBus.current.collectAsState()
    val lang = Glass.lang
    val t = updateTexts(lang)
    val status = when (val s = state) {
        UpdateState.Checking -> t.checking
        UpdateState.UpToDate -> t.upToDate
        is UpdateState.Available -> t.version(s.release.version)
        is UpdateState.Downloading -> t.downloading
        is UpdateState.Installing -> t.installing
        is UpdateState.Failed -> s.message ?: t.failed
        UpdateState.Idle -> t.current(BuildConfig.VERSION_NAME)
    }
    ListRow(
        t.check, subtitle = status, icon = Icons.Rounded.SystemUpdate, iconColor = Color(0xFF34C759), chevron = true,
        onClick = {
            when (val s = state) {
                is UpdateState.Available -> startUpdate(ctx, scope, s.release, lang)
                is UpdateState.Downloading, is UpdateState.Installing, UpdateState.Checking -> Unit
                else -> checkForUpdate(ctx, scope, manual = true)
            }
        },
    )
}
