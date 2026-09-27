package app.funput.funput.ui.kit.rows

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.max

/** Slot ids the row layout recognises; anything else is ignored. */
internal enum class RowSlot { LEADING, BODY, DETAIL, ACCESSORY }

/** A detail may take at most this share of the width beside the body before it moves below. */
private const val DetailShare = 0.45f

/**
 * Lays out a row: leading icon, body (title and summary), detail (a value), accessory (chevron,
 * toggle, check).
 *
 * The detail sits beside the body when both fit: the detail is at most [DetailShare] of the free
 * width and the body keeps room for its longest word. Otherwise the detail moves under the body.
 * This is what keeps long Vietnamese labels, large font scales and narrow phones from squeezing
 * a title into a column of one-word lines, which is how the old rows broke. The accessory never
 * moves: it stays at the end, vertically centred.
 */
@Composable
internal fun RowLayout(
    gap: Dp,
    stackGap: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val stackGapPx = stackGap.roundToPx()
        val width = constraints.maxWidth
        fun slot(id: RowSlot): Measurable? = measurables.firstOrNull { it.layoutId == id }
        val loose = Constraints(maxWidth = width)
        val leading = slot(RowSlot.LEADING)?.measure(loose)
        val accessory = slot(RowSlot.ACCESSORY)?.measure(loose)
        val start = leading?.let { it.width + gapPx } ?: 0
        val inner = max(0, width - start - (accessory?.let { it.width + gapPx } ?: 0))

        val body = slot(RowSlot.BODY) ?: error("A row needs a body")
        val detailSlot = slot(RowSlot.DETAIL)
        val detailWidth = detailSlot?.maxIntrinsicWidth(Constraints.Infinity) ?: 0
        val bodyMinWidth = body.minIntrinsicWidth(Constraints.Infinity)
        val beside = detailSlot == null ||
            (detailWidth <= inner * DetailShare && inner - detailWidth - gapPx >= bodyMinWidth)

        val detail: Placeable?
        val bodyPlaceable: Placeable
        if (beside) {
            detail = detailSlot?.measure(Constraints(maxWidth = detailWidth))
            val detailSpace = detail?.let { it.width + gapPx } ?: 0
            bodyPlaceable = body.measure(Constraints(maxWidth = max(0, inner - detailSpace)))
        } else {
            bodyPlaceable = body.measure(Constraints(maxWidth = inner))
            detail = detailSlot?.measure(Constraints(maxWidth = inner))
        }
        val content = if (beside) max(bodyPlaceable.height, detail?.height ?: 0)
        else bodyPlaceable.height + stackGapPx + (detail?.height ?: 0)
        val height = maxOf(content, leading?.height ?: 0, accessory?.height ?: 0, constraints.minHeight)

        layout(width, height) {
            fun Placeable.centerY() = (height - this.height) / 2
            leading?.place(0, if (beside) leading.centerY() else 0)
            if (beside) {
                bodyPlaceable.place(start, bodyPlaceable.centerY())
                detail?.let { it.place(start + inner - it.width, it.centerY()) }
            } else {
                val top = (height - content) / 2
                bodyPlaceable.place(start, top)
                detail?.place(start, top + bodyPlaceable.height + stackGapPx)
            }
            accessory?.let { it.place(width - it.width, it.centerY()) }
        }
    }
}

/** Tags a child of [RowLayout] with its [slot]. */
internal fun Modifier.rowSlot(slot: RowSlot): Modifier = layoutId(slot)
