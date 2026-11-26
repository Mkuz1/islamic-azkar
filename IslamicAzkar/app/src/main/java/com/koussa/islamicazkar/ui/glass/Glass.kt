package com.koussa.islamicazkar.ui.glass

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/** Eight-pointed star (Rub el Hizb) outline, used for ornaments and patterns. */
fun starPath(c: Offset, r: Float, inner: Float = 0.62f): Path = Path().apply {
    for (i in 0 until 16) {
        val ang = Math.PI / 8 * i - Math.PI / 2
        val rr = if (i % 2 == 0) r else r * inner
        val p = Offset(c.x + (rr * cos(ang)).toFloat(), c.y + (rr * sin(ang)).toFloat())
        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

/**
 * The calm, static backdrop the glass refracts: the Mushaf's green and gold as soft light,
 * over its faint geometric pattern. No motion, no rainbow — just enough for glass to read.
 */
@Composable
fun LiquidWallpaper(modifier: Modifier = Modifier) {
    val g = Glass.colors
    androidx.compose.foundation.Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(g.wallTop, g.wallBottom)))
        val r = maxOf(size.width, size.height)
        val green = Offset(size.width * 0.1f, size.height * 0.05f)
        drawCircle(Brush.radialGradient(listOf(g.glowGreen, Color.Transparent), green, r * 0.75f), r * 0.75f, green)
        val gold = Offset(size.width * 0.95f, size.height * 0.72f)
        drawCircle(Brush.radialGradient(listOf(g.glowGold, Color.Transparent), gold, r * 0.6f), r * 0.6f, gold)
        // Interlaced eight-pointed stars, as on the Mushaf's opening page.
        val step = 72.dp.toPx()
        val stroke = Stroke(1.dp.toPx())
        var y = 0f
        while (y < size.height + step) {
            var x = 0f
            while (x < size.width + step) {
                drawPath(starPath(Offset(x, y), step * 0.5f, 0.78f), g.pattern, style = stroke)
                drawRect(g.pattern, Offset(x - step * 0.18f, y - step * 0.18f), androidx.compose.ui.geometry.Size(step * 0.36f, step * 0.36f), style = stroke)
                x += step
            }
            y += step
        }
    }
}

@Deprecated("Use LiquidWallpaper", ReplaceWith("LiquidWallpaper(modifier)"))
@Composable
fun AppBackground(modifier: Modifier = Modifier) = LiquidWallpaper(modifier)

// ───────────────────────── Backdrop (for the navigation layer) ─────────────────────────

/**
 * Captures the content under floating chrome into a GraphicsLayer so the tab bar,
 * back button and sheets can show it blurred — Apple's Liquid Glass navigation layer.
 */
@Stable
class Backdrop(val layer: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
}

val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

val blurSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun rememberBackdrop(): Backdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { Backdrop(layer) }
}

fun Modifier.backdropSource(b: Backdrop): Modifier = this
    .onGloballyPositioned { b.origin = it.positionInRoot() }
    .drawWithContent {
        // Layers need a hardware canvas; software draws (e.g. View.draw to a bitmap) just paint directly.
        if (drawContext.canvas.nativeCanvas.isHardwareAccelerated) {
            b.layer.record { this@drawWithContent.drawContent() }
            drawLayer(b.layer)
        } else {
            drawContent()
        }
    }

/**
 * Liquid Glass: the backdrop behind the shape, blurred and (Android 13+) bent at the rim
 * like thick glass, under a translucent tint, with a light-catching edge.
 * [tint] / [fallback] override the fill (used for content panes and the opening card).
 */
fun Modifier.glass(
    shape: Shape,
    strong: Boolean = false,
    blurRadius: Dp = 24.dp,
    elevation: Dp = 0.dp,
    tint: Color? = null,
    fallback: Color? = null,
): Modifier = composed {
    val g = Glass.colors
    val backdrop = LocalBackdrop.current
    val blurLayer = rememberGraphicsLayer()
    var pos by remember { mutableStateOf(Offset.Zero) }
    val useBlur = backdrop != null && blurSupported
    val dir = LocalLayoutDirection.current

    this
        .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape, clip = false, ambientColor = g.shadow, spotColor = g.shadow) else Modifier)
        .onGloballyPositioned { pos = it.positionInRoot() }
        .drawWithCache {
            val outline = shape.createOutline(size, dir, this)
            val clip = Path().apply { addOutline(outline) }
            // Tinted glass (the opening card, dialogs) gets a softer sheen so its colour stays rich.
            val shine = if (tint != null) g.highlight.copy(alpha = minOf(g.highlight.alpha, 0.12f)) else g.highlight
            val sheen = Brush.verticalGradient(0f to shine, 0.45f to Color.Transparent)
            // Light catches the top-leading and bottom-trailing edges, as on Apple's glass.
            val rim = Brush.linearGradient(
                0f to g.rimLight, 0.35f to g.rimDark, 0.7f to g.rimDark, 1f to g.rimLight.copy(alpha = g.rimLight.alpha * 0.6f),
                start = Offset.Zero, end = Offset(size.width, size.height),
            )
            val corner = (outline as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: 0f
            val effect = if (useBlur) liquidGlassEffect(size, corner, blurRadius.toPx(), 12.dp.toPx()) else null
            val stroke = Stroke(width = 1.dp.toPx())
            val blurFill = tint ?: if (strong) g.fillStrong else g.tint
            val opaqueFill = fallback ?: if (g.dark) Color(0xF21C1C1E) else Color(0xF5F9F9F9)
            onDrawWithContent {
                val blurNow = useBlur && drawContext.canvas.nativeCanvas.isHardwareAccelerated
                clipPath(clip) {
                    if (blurNow && backdrop != null) {
                        blurLayer.renderEffect = effect
                        blurLayer.record {
                            translate(backdrop.origin.x - pos.x, backdrop.origin.y - pos.y) { drawLayer(backdrop.layer) }
                        }
                        drawLayer(blurLayer)
                        drawRect(blurFill)
                    } else {
                        drawRect(opaqueFill)
                    }
                    drawRect(sheen)
                }
                drawContent()
                drawOutline(outline, rim, style = stroke)
            }
        }
}

private fun Path.addOutline(o: Outline) {
    when (o) {
        is Outline.Rectangle -> addRect(o.rect)
        is Outline.Rounded -> addRoundRect(o.roundRect)
        is Outline.Generic -> addPath(o.path)
    }
}

/** A content pane (card, list group) made of Liquid Glass. */
fun Modifier.pane(shape: Shape): Modifier = composed {
    val g = Glass.colors
    glass(shape, blurRadius = 18.dp, tint = g.pane, fallback = g.paneFallback)
}

/** iOS-style press feedback: a slight dim, and a gentle squish for prominent controls. With a TV
 *  remote, a keyboard or a game pad, the control in focus is also lifted and outlined ([focusRing]). */
fun Modifier.pressable(onClick: () -> Unit, enabled: Boolean = true, squish: Boolean = false): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed && squish) 0.96f else 1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "press")
    val a by animateFloatAsState(if (pressed) 0.55f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "dim")
    this
        .focusRing(source)
        .scale(s)
        .alpha(a)
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/** For single-line text fields: the remote's Up / Down leave the field instead of moving the cursor. */
fun Modifier.dpadLeavesField(): Modifier = composed {
    val fm = androidx.compose.ui.platform.LocalFocusManager.current
    onPreviewKeyEvent { e ->
        if (e.type != androidx.compose.ui.input.key.KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (e.key) {
            androidx.compose.ui.input.key.Key.DirectionDown -> fm.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
            androidx.compose.ui.input.key.Key.DirectionUp -> fm.moveFocus(androidx.compose.ui.focus.FocusDirection.Up)
            else -> false
        }
    }
}

/** A clickable with no touch ripple but with the focus highlight for remotes and keyboards. */
fun Modifier.tvClickable(onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    focusRing(src).clickable(src, null, onClick = onClick)
}

/**
 * Focus highlight for the TV remote (D-pad), keyboards and game pads: the focused control is
 * lifted a little and drawn with a bright outline and a soft fill. Touch screens never focus
 * these controls, so phones look exactly as before.
 */
fun Modifier.focusRing(source: MutableInteractionSource): Modifier = composed {
    val g = Glass.colors
    val focused by source.collectIsFocusedAsState()
    val t by animateFloatAsState(if (focused) 1f else 0f, spring(dampingRatio = 0.8f, stiffness = 600f), label = "focus")
    val ring = if (g.dark) Color.White else g.accent
    this
        .graphicsLayer {
            // Small controls lift more than large cards (at most ~10 dp wider).
            val lift = if (size.width > 0f) minOf(0.06f, 10.dp.toPx() / size.width) else 0f
            scaleX = 1f + lift * t; scaleY = 1f + lift * t
            shadowElevation = 10.dp.toPx() * t
            shape = RoundedCornerShape(16.dp); clip = false
        }
        .drawWithContent {
            drawContent()
            if (t > 0.01f) {
                val sw = 3.dp.toPx()
                val r = minOf(16.dp.toPx(), size.minDimension / 2)
                drawRoundRect(ring.copy(alpha = 0.12f * t), cornerRadius = CornerRadius(r))
                drawRoundRect(
                    ring.copy(alpha = t), topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height - sw),
                    cornerRadius = CornerRadius(maxOf(r - sw / 2, 0f)), style = Stroke(sw),
                )
            }
        }
}

/** Round glass button for the navigation layer (back, close, language). */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = Glass.colors.text,
    onClick: () -> Unit,
) {
    Box(
        modifier.size(size).pressable(onClick, squish = true).glass(CircleShape, elevation = 6.dp),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.46f)) }
}

/** Filled capsule button in the app tint (iOS "borderedProminent"). */
@Composable
fun AccentButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    val g = Glass.colors
    Row(
        modifier
            .pressable(onClick, squish = true)
            .clip(RoundedCornerShape(50))
            .background(g.accent)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = g.onAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = g.onAccent, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    }
}

/** UISegmentedControl: gray track, raised thumb that slides to the selection. */
@Composable
fun GlassSegmented(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val g = Glass.colors
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(50))
            .background(g.fillGray)
            .padding(2.dp),
    ) {
        val segW = maxWidth / options.size
        val x by animateDpAsState(segW * selected, spring(dampingRatio = 0.8f, stiffness = 500f), label = "seg")
        Box(
            Modifier
                .offset(x = x) // offset() already mirrors in RTL
                .width(segW)
                .fillMaxHeight()
                .shadow(2.dp, RoundedCornerShape(50), ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
                .clip(RoundedCornerShape(50))
                .background(if (g.dark) Color(0xFF636366) else Color.White),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                val src = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .focusRing(src)
                        .clickable(src, null) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = g.text, textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

data class TabItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

/** iOS 26 floating tab bar: a glass capsule with a soft lens behind the selected tab. */
@Composable
fun GlassTabBar(items: List<TabItem>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val g = Glass.colors
    BoxWithConstraints(
        modifier
            .height(62.dp)
            .glass(RoundedCornerShape(50), elevation = 14.dp)
            .padding(4.dp),
    ) {
        val segW = maxWidth / items.size
        val x by animateDpAsState(segW * selected, spring(dampingRatio = 0.75f, stiffness = 420f), label = "tab")
        Box(
            Modifier
                .offset(x = x) // offset() already mirrors in RTL
                .width(segW)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(g.fillGray),
        )
        Row(Modifier.fillMaxSize()) {
            items.forEachIndexed { i, item ->
                val sel = i == selected
                val src = remember { MutableInteractionSource() }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .focusRing(src)
                        .clickable(src, null) { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        if (sel) item.selectedIcon else item.icon, item.label,
                        tint = if (sel) g.accent else g.text,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        item.label, fontSize = 10.sp, maxLines = 1,
                        color = if (sel) g.accent else g.text,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/** Circular progress ring. */
@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier, stroke: Dp = 3.dp, track: Color = Glass.colors.fillGray, color: Color = Glass.colors.accent) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), spring(0.85f, 300f), label = "ring")
    Canvas(modifier) {
        val sw = stroke.toPx()
        val inset = sw / 2
        val arcSize = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(sw))
        drawArc(color, -90f, 360f * p, false, Offset(inset, inset), arcSize, style = Stroke(sw, cap = StrokeCap.Round))
    }
}

/** 0.5pt separator. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(0.5.dp).background(Glass.colors.separator))
}
