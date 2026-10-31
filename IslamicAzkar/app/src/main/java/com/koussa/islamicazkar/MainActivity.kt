package com.koussa.islamicazkar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.koussa.islamicazkar.data.Reading
import com.koussa.islamicazkar.data.Settings
import com.koussa.islamicazkar.data.ThemeMode
import com.koussa.islamicazkar.notify.Reminders
import com.koussa.islamicazkar.ui.AppState
import com.koussa.islamicazkar.ui.AzkarApp
import com.koussa.islamicazkar.ui.DedicationScreen
import com.koussa.islamicazkar.ui.LocalHinge
import com.koussa.islamicazkar.ui.rememberHinge
import androidx.compose.runtime.CompositionLocalProvider
import com.koussa.islamicazkar.ui.glass.AzkarTheme

class MainActivity : ComponentActivity() {
    private val settings by lazy { Settings.get(this) }
    private var deepLink by mutableStateOf<Pair<Reading, Int?>?>(null)
    private var openTab by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Reminders.reschedule(this)
        com.koussa.islamicazkar.widget.WidgetTicker.schedule(this)
        if (savedInstanceState == null) { deepLink = parse(intent); openTab = tabOf(intent) }

        setContent {
            val st = settings
            // The dedication opens the app, like the Mushaf; deep links from widgets/notifications skip it.
            var showDedication by rememberSaveable { mutableStateOf(deepLink == null && openTab == null) }
            val app = remember { AppState(st) }
            val dark = when (st.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !showDedication && !dark
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = !showDedication && !dark
            }
            deepLink?.let { (reading, focus) ->
                SideEffect { app.open(reading, focus); deepLink = null; showDedication = false }
            }

            openTab?.let { t ->
                SideEffect { app.tab = t; openTab = null; showDedication = false }
            }

            val hinge = rememberHinge(this)
            // Widgets follow the app language.
            androidx.compose.runtime.LaunchedEffect(st.lang) {
                com.koussa.islamicazkar.widget.Widgets.refreshEverything(this@MainActivity)
            }
            AzkarTheme(st.theme, st.lang) { CompositionLocalProvider(LocalHinge provides hinge) {
                AnimatedContent(
                    targetState = showDedication,
                    transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(500)) },
                    label = "intro",
                ) { intro ->
                    if (intro) DedicationScreen(onContinue = { showDedication = false }, onOpen = { app.open(it); showDedication = false }) else AzkarApp(app)
                }
            } }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        parse(intent)?.let { deepLink = it }
        tabOf(intent)?.let { openTab = it }
    }

    override fun onResume() {
        super.onResume()
        settings.reloadTasbih() // the tasbih widget may have counted while we were away
    }

    private fun parse(intent: Intent?): Pair<Reading, Int?>? {
        val reading = Reading.fromKey(intent?.getStringExtra(EXTRA_OPEN)) ?: return null
        val focus = intent?.getIntExtra(EXTRA_FOCUS, -1)?.takeIf { it >= 0 }
        return reading to focus
    }

    private fun tabOf(intent: Intent?): Int? = intent?.getIntExtra(EXTRA_TAB, -1)?.takeIf { it >= 0 }

    companion object {
        const val EXTRA_TAB = "tab"
        const val EXTRA_OPEN = "open"
        const val EXTRA_FOCUS = "focus"
    }
}
