package com.koussa.islamicazkar.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.map

/**
 * Where a foldable's hinge sits, in dp from the window's top-left. A vertical hinge
 * (book posture / unfolded) splits panes left and right; a horizontal half-opened
 * hinge is tabletop posture.
 */
@Immutable
data class Hinge(
    val vertical: Boolean,
    val start: Dp,
    val end: Dp,
    val separating: Boolean,
    val halfOpened: Boolean,
)

val LocalHinge = staticCompositionLocalOf<Hinge?> { null }

/** Observes the activity's folding feature (null on regular phones and tablets). */
@Composable
fun rememberHinge(activity: Activity): Hinge? {
    val density = LocalDensity.current
    val flow = remember(activity) {
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).map { info ->
            info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()?.let { f ->
                with(density) {
                    val vertical = f.orientation == FoldingFeature.Orientation.VERTICAL
                    Hinge(
                        vertical = vertical,
                        start = (if (vertical) f.bounds.left else f.bounds.top).toDp(),
                        end = (if (vertical) f.bounds.right else f.bounds.bottom).toDp(),
                        separating = f.isSeparating,
                        halfOpened = f.state == FoldingFeature.State.HALF_OPENED,
                    )
                }
            }
        }
    }
    val hinge by flow.collectAsState(initial = null)
    return hinge
}

/** Width at which the Home tab becomes a two-pane, iPad-style split view. */
val WideBreakpoint: Dp = 600.dp

/** Comfortable reading column on large screens. */
val ReadingMaxWidth: Dp = 760.dp
