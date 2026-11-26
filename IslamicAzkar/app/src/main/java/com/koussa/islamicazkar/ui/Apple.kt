package com.koussa.islamicazkar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koussa.islamicazkar.data.Lang
import com.koussa.islamicazkar.ui.glass.Glass
import com.koussa.islamicazkar.ui.glass.Hairline
import com.koussa.islamicazkar.ui.glass.pane
import com.koussa.islamicazkar.ui.glass.pressable
import com.koussa.islamicazkar.ui.glass.dpadLeavesField

/** Corner radius of iOS 26 inset-grouped sections and cards. */
val GroupShape = RoundedCornerShape(22.dp)

/** Section header above an inset group, in the footnote style of iOS lists. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    val lang = Glass.lang
    Text(
        if (lang == Lang.AR) text else text.uppercase(),
        color = Glass.colors.text2, fontSize = 13.sp,
        letterSpacing = if (lang == Lang.AR) 0.sp else 0.2.sp,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 7.dp),
    )
}

/** Footer text under an inset group. */
@Composable
fun SectionFooter(text: String, modifier: Modifier = Modifier) {
    Text(
        text, color = Glass.colors.text2, fontSize = 13.sp, lineHeight = 18.sp,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 7.dp),
    )
}

/** An inset-grouped section: rows on a white (or #1C1C1E) rounded surface. */
@Composable
fun InsetGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().pane(GroupShape), content = content)
}

/** Separator between rows, inset past the leading icon like UITableView. */
@Composable
fun RowDivider(inset: Dp = 58.dp) {
    Hairline(Modifier.padding(start = inset))
}

/** Rounded square with a white glyph — the iOS Settings icon style, on solid system colours. */
@Composable
fun SymbolSquare(icon: ImageVector, color: Color, size: Dp = 30.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.24f)).background(color),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.62f)) }
}

/** A list row: optional icon, title / subtitle, trailing value and disclosure chevron. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color = Glass.colors.accent,
    subtitle: String? = null,
    subtitleFont: androidx.compose.ui.text.font.FontFamily? = null,
    value: String? = null,
    chevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val g = Glass.colors
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(onClick) else Modifier)
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            SymbolSquare(icon, iconColor)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = g.text, fontSize = 17.sp, lineHeight = 22.sp)
            if (subtitle != null) {
                Text(
                    subtitle, color = g.text2, fontSize = if (subtitleFont != null) 15.sp else 13.sp,
                    fontFamily = subtitleFont, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Text(value, color = g.text2, fontSize = 17.sp, maxLines = 1)
        }
        trailing?.let { Spacer(Modifier.width(8.dp)); it() }
        if (chevron) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = g.text3, modifier = Modifier.size(22.dp))
        }
    }
}

/** Plain content card (no glass) on the grouped background. */
@Composable
fun Card(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .then(if (onClick != null) Modifier.pressable(onClick, squish = true) else Modifier)
            .pane(GroupShape)
            .padding(padding),
        content = content,
    )
}

/** UISearchBar: a gray field with a magnifier and a clear button. */
@Composable
fun SearchBar(value: String, hint: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val g = Glass.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .pane(RoundedCornerShape(50))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.Search, null, tint = g.text2, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, color = g.text2, fontSize = 17.sp, maxLines = 1)
            BasicTextField(
                value, onChange, singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = g.text, fontSize = 17.sp),
                cursorBrush = SolidColor(g.accent),
                modifier = Modifier.fillMaxWidth().dpadLeavesField(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(Icons.Rounded.Cancel, null, tint = g.text3, modifier = Modifier.size(18.dp).pressable({ onChange("") }))
        }
    }
}

/** Small gray capsule label (e.g. "1 / 23"). */
@Composable
fun Capsule(text: String, modifier: Modifier = Modifier, color: Color = Glass.colors.text2, background: Color = Glass.colors.fillGray) {
    Text(
        text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = modifier.clip(RoundedCornerShape(50)).background(background).padding(horizontal = 9.dp, vertical = 3.dp),
    )
}
