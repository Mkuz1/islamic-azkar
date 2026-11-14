package com.koussa.islamicazkar.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import com.koussa.islamicazkar.ui.glass.Hairline
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koussa.islamicazkar.data.Reading
import com.koussa.islamicazkar.data.Settings
import com.koussa.islamicazkar.data.Zikr
import com.koussa.islamicazkar.ui.glass.Backdrop
import com.koussa.islamicazkar.ui.glass.Glass
import com.koussa.islamicazkar.ui.glass.GlassTabBar
import com.koussa.islamicazkar.ui.glass.LiquidWallpaper
import com.koussa.islamicazkar.ui.glass.LocalBackdrop
import com.koussa.islamicazkar.ui.glass.TabItem
import com.koussa.islamicazkar.ui.glass.backdropSource
import com.koussa.islamicazkar.ui.glass.blurSupported
import com.koussa.islamicazkar.ui.glass.glass
import com.koussa.islamicazkar.ui.glass.rememberBackdrop
import kotlinx.coroutines.delay

sealed interface Route {
    val key: String

    data object Home : Route { override val key = "home" }
    data object Favorites : Route { override val key = "fav" }
    data object Tasbih : Route { override val key = "tasbih" }
    data object WidgetsRoute : Route { override val key = "widgets" }
    data object RecordRoute : Route { override val key = "record" }
    data object SettingsRoute : Route { override val key = "settings" }
    data class Category(val catKey: String) : Route { override val key = "cat:$catKey" }
    data class GuideRoute(val guideKey: String) : Route { override val key = "guide:$guideKey" }
    data class Reader(val reading: Reading, val focusZikr: Int? = null) : Route { override val key = "read:${reading.key()}" }
}

/** UI-wide navigation and overlay state. */
@Stable
class AppState(val settings: Settings) {
    var tab by mutableIntStateOf(0)
    private val stacks = List(5) { mutableStateListOf<Route>() }
    private val roots = listOf(Route.Home, Route.Favorites, Route.Tasbih, Route.RecordRoute, Route.SettingsRoute)

    /** True when the window is wide enough (tablet, unfolded foldable) for a split view. */
    var wide by mutableStateOf(false)

    val current: Route get() = stacks[tab].lastOrNull() ?: roots[tab]
    val depth: Int get() = stacks[tab].size

    /** Split view: the navigation pane shows everything but readers… */
    val listRoute: Route get() = stacks[tab].lastOrNull { it !is Route.Reader } ?: roots[tab]
    /** …and the detail pane shows the reader, if one is open. */
    val detailRoute: Route.Reader? get() = stacks[tab].lastOrNull() as? Route.Reader

    val showBack: Boolean get() = if (wide && tab == 0) stacks[tab].any { it !is Route.Reader } else stacks[tab].isNotEmpty()

    var shareTarget by mutableStateOf<Pair<Zikr, String>?>(null)
    var celebration by mutableStateOf<Celebration?>(null)
    var toast by mutableStateOf<String?>(null)

    /** Collapsed inline title shown in the navigation bar once the large title scrolls away (iOS style). */
    var inlineTitle by mutableStateOf<String?>(null)

    fun push(r: Route) {
        if (wide && r is Route.Reader) stacks[tab].removeAll { it is Route.Reader }
        stacks[tab].add(r)
    }

    fun pop(): Boolean {
        val s = stacks[tab]
        if (s.isEmpty()) return false
        // In a split view, "back" walks the navigation pane; the open reader stays put.
        val i = if (wide && tab == 0) s.indexOfLast { it !is Route.Reader } else s.lastIndex
        s.removeAt(if (i >= 0) i else s.lastIndex)
        return true
    }

    fun selectTab(i: Int) { if (i == tab) stacks[i].clear() else tab = i }

    fun open(reading: Reading, focus: Int? = null) {
        tab = 0
        stacks[0].clear()
        stacks[0].add(Route.Reader(reading, focus))
    }
}

val LocalApp = staticCompositionLocalOf<AppState> { error("no app state") }

@Composable
fun AzkarApp(state: AppState) {
    val backdrop = rememberBackdrop()
    val wallpaper = rememberBackdrop()
    val s = Glass.s
    val hinge = LocalHinge.current

    BackHandler(enabled = state.celebration != null || state.shareTarget != null || state.depth > 0 || state.tab != 0) {
        when {
            state.celebration != null -> state.celebration = null
            state.shareTarget != null -> state.shareTarget = null
            state.pop() -> Unit
            else -> state.tab = 0
        }
    }

    CompositionLocalProvider(LocalApp provides state) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // A separating hinge (dual-screen devices) keeps the tab bar on one screen.
            val bookHinge = hinge?.takeIf { it.vertical && it.separating && it.end > it.start && it.start in 240.dp..(maxWidth - 240.dp) }
            val leftWidth = bookHinge?.start ?: maxWidth
            val gap = bookHinge?.let { it.end - it.start } ?: 0.dp

            // Everything in here is what the floating glass refracts. One full-width page on
            // every screen size — phones, unfolded foldables and tablets — with content centred.
            Box(Modifier.fillMaxSize().backdropSource(backdrop)) {
                // Content panes refract the wallpaper; floating chrome refracts everything.
                Box(Modifier.fillMaxSize().backdropSource(wallpaper)) { LiquidWallpaper() }
                // On TV the pages start below the tab bar: the remote's Down from the bar must find
                // the page's content below it (a list under the bar would count as overlapping it).
                CompositionLocalProvider(LocalBackdrop provides wallpaper) {
                    Box(if (isTv()) Modifier.fillMaxSize().padding(top = 96.dp) else Modifier.fillMaxSize()) { Pane(state.current) }
                }
            }

            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                TopBlur(backdrop, Modifier.align(Alignment.TopCenter))
                InlineTitle(state.inlineTitle, Modifier.align(Alignment.TopCenter))

                val tv = isTv()
                if (state.showBack) {
                    BackChrome(
                        if (tv) Modifier.align(Alignment.TopStart).padding(start = 48.dp, top = 30.dp)
                        else Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 16.dp, top = 6.dp),
                    ) { state.pop() }
                }

                // With a separating hinge, keep the bar on one screen instead of straddling the gap.
                // On TV the bar sits at the top, where the remote's Up button reaches it from any page.
                val barModifier = if (tv) {
                    Modifier.align(Alignment.TopCenter).padding(top = 24.dp).widthIn(max = 620.dp).fillMaxWidth()
                } else if (bookHinge != null && gap > 0.dp) {
                    Modifier.align(Alignment.BottomStart).width(leftWidth)
                } else {
                    Modifier.align(Alignment.BottomCenter).widthIn(max = 520.dp).fillMaxWidth()
                }
                GlassTabBar(
                    items = listOf(
                        TabItem(s.tabHome, Icons.Outlined.Home, Icons.Rounded.Home),
                        TabItem(s.tabFavorites, Icons.Outlined.FavoriteBorder, Icons.Rounded.Favorite),
                        TabItem(s.tabTasbih, Icons.Outlined.TouchApp, Icons.Rounded.TouchApp),
                        TabItem(HistoryTexts.of(Glass.lang).tab, Icons.Outlined.CalendarMonth, Icons.Rounded.CalendarMonth),
                        TabItem(s.tabSettings, Icons.Outlined.Settings, Icons.Rounded.Settings),
                    ),
                    selected = state.tab,
                    onSelect = state::selectTab,
                    modifier = if (tv) barModifier.padding(horizontal = 18.dp) else barModifier
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )

                UpdateHost(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 92.dp))

                ToastHost(state, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 92.dp))

                state.shareTarget?.let { (z, title) ->
                    ShareSheet(z, title, onDismiss = { state.shareTarget = null })
                }

                state.celebration?.let { CelebrationCard(it) { state.celebration = null } }
            }
        }
    }
}

private fun defaultReading(): Reading = when (currentMoment()) {
    Moment.MORNING -> Reading.Morning
    Moment.NOON -> Reading.OfChapter(25)
    Moment.EVENING -> Reading.Evening
    Moment.NIGHT -> Reading.OfChapter(28)
}

/** One navigation pane: animated route changes, saved scroll state, and a centred reading column. */
@Composable
private fun Pane(route: Route) {
    val saveable = rememberSaveableStateHolder()
    AnimatedContent(
        targetState = route,
        transitionSpec = {
            (fadeIn(tween(260)) + scaleIn(tween(320), initialScale = 0.985f)) togetherWith fadeOut(tween(180))
        },
        contentKey = { it.key },
        label = "screens",
    ) { r ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = ReadingMaxWidth).fillMaxSize()) {
                saveable.SaveableStateProvider(r.key) {
                    when (r) {
                        Route.Home -> HomeScreen()
                        Route.Favorites -> FavoritesScreen()
                        Route.Tasbih -> TasbihScreen()
                        Route.WidgetsRoute -> WidgetsScreen()
                        Route.RecordRoute -> HistoryScreen()
                        Route.SettingsRoute -> SettingsScreen()
                        is Route.Category -> CategoryScreen(r.catKey)
                        is Route.GuideRoute -> GuideScreen(r.guideKey)
                        is Route.Reader -> ReaderScreen(r.reading, r.focusZikr)
                    }
                }
            }
        }
    }
}

/** The iOS navigation-bar title that fades in once the large title has scrolled away. */
@Composable
private fun InlineTitle(title: String?, modifier: Modifier) {
    val g = Glass.colors
    var last by remember { mutableStateOf("") }
    if (title != null) last = title
    AnimatedVisibility(title != null, modifier = modifier.statusBarsPadding().padding(top = 16.dp), enter = fadeIn(tween(180)), exit = fadeOut(tween(150))) {
        Text(
            last, color = g.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 76.dp),
        )
    }
}

/** Screens call this so their large title collapses into the navigation bar while scrolling. */
@Composable
fun CollapsingTitle(title: String, listState: LazyListState) {
    val app = LocalApp.current
    val collapsed by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 120 }
    }
    LaunchedEffect(collapsed, title) { app.inlineTitle = if (collapsed) title else null }
    DisposableEffect(Unit) { onDispose { if (app.inlineTitle == title) app.inlineTitle = null } }
}

/**
 * Progressive blur under the status bar: a blurred copy of the content that fades out
 * downward, so text scrolls softly beneath the clock and icons.
 */
@Composable
private fun TopBlur(backdrop: Backdrop, modifier: Modifier) {
    val g = Glass.colors
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val blurLayer = rememberGraphicsLayer()
    var pos by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier
            .fillMaxWidth()
            .height(top + 34.dp)
            .onGloballyPositioned { pos = it.positionInRoot() }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithCache {
                val mask = Brush.verticalGradient(0f to Color.Black, 0.55f to Color.Black, 1f to Color.Transparent)
                val tint = Brush.verticalGradient(listOf(g.background.copy(alpha = if (blurSupported) 0.55f else 0.94f), g.background.copy(alpha = 0f)))
                onDrawBehind {
                    // iOS scroll-edge effect: only where real blur is available; otherwise draw nothing.
                    if (blurSupported && drawContext.canvas.nativeCanvas.isHardwareAccelerated) {
                        blurLayer.renderEffect = BlurEffect(18.dp.toPx(), 18.dp.toPx(), TileMode.Clamp)
                        blurLayer.record {
                            translate(backdrop.origin.x - pos.x, backdrop.origin.y - pos.y) { drawLayer(backdrop.layer) }
                        }
                        drawLayer(blurLayer)
                        drawRect(tint)
                        drawRect(mask, blendMode = BlendMode.DstIn)
                    }
                }
            },
    )
}

@Composable
private fun BackChrome(modifier: Modifier, onBack: () -> Unit) {
    com.koussa.islamicazkar.ui.glass.GlassIconButton(
        icon = Icons.AutoMirrored.Rounded.ArrowBackIos,
        contentDescription = "Back", modifier = modifier, onClick = onBack,
    )
}

@Composable
private fun ToastHost(state: AppState, modifier: Modifier) {
    val g = Glass.colors
    var shown by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.toast) {
        val msg = state.toast ?: return@LaunchedEffect
        shown = msg
        delay(1600)
        state.toast = null
        shown = null
    }
    AnimatedVisibility(
        visible = shown != null,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        Text(
            shown.orEmpty(),
            color = g.text, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier
                .glass(RoundedCornerShape(50), strong = true, elevation = 8.dp)
                .padding(horizontal = 20.dp, vertical = 11.dp),
        )
    }
}
