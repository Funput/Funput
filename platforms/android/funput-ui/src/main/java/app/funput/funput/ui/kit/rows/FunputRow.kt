package app.funput.funput.ui.kit.rows

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.DisabledAlpha
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.kit.tokens.OpacityTokens

/** Rows are never shorter than this, which also keeps every row a comfortable touch target. */
private val RowMinHeight = 56.dp

/**
 * The shared anatomy of every FunputUI row, inside a card: an optional tinted icon tile, a title
 * with an optional summary, an optional detail that moves below the title when space runs out
 * (see [RowLayout]), and an optional accessory at the end.
 *
 * Interaction (click, toggle, select) and its semantics belong to [modifier], set by the specific
 * row, so the whole row is one target and TalkBack reads it as one item.
 */
@Composable
fun FunputRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    detail: (@Composable () -> Unit)? = null,
    accessory: (@Composable () -> Unit)? = null,
) {
    val colors = FunputUi.colors
    val type = FunputUi.typography
    val spacing = FunputUi.spacing
    RowLayout(
        gap = spacing.medium,
        stackGap = spacing.tight,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = RowMinHeight)
            .padding(horizontal = spacing.cardPadding, vertical = spacing.medium)
            .alpha(if (enabled) 1f else DisabledAlpha),
    ) {
        icon?.let { RowIcon(it, Modifier.rowSlot(RowSlot.LEADING)) }
        Column(Modifier.rowSlot(RowSlot.BODY)) {
            BasicText(title, style = type.body.copy(color = colors.label))
            summary?.let { BasicText(it, style = type.caption.copy(color = colors.secondaryLabel)) }
        }
        detail?.let { Box(Modifier.rowSlot(RowSlot.DETAIL)) { it() } }
        accessory?.let { Box(Modifier.rowSlot(RowSlot.ACCESSORY)) { it() } }
    }
}

/** The accent-tinted square behind a row icon. */
@Composable
private fun RowIcon(@DrawableRes icon: Int, modifier: Modifier) {
    val accent = FunputUi.colors.accent
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(32.dp)
            .clip(FunputUi.shapes.iconTile)
            .background(accent.copy(alpha = OpacityTokens.tintFill)),
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(accent),
            modifier = Modifier.size(20.dp),
        )
    }
}
