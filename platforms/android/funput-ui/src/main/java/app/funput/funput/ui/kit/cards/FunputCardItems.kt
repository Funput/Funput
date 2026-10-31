package app.funput.funput.ui.kit.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * One card whose rows are lazy list items, for a list too long to compose at once (thousands of
 * words, say). Each row draws its own slice of the card, so together they read as a [FunputCard]
 * with [FunputDivider]s, while only the rows on screen exist.
 *
 * The rows must touch: use it in a `FunputScreen` with no item spacing.
 */
fun <T> LazyListScope.funputCardItems(
    items: List<T>,
    key: (T) -> Any,
    content: @Composable (T) -> Unit,
) {
    itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
        Column(Modifier.fillMaxWidth().cardSlice(isFirst = index == 0, isLast = index == items.lastIndex)) {
            if (index > 0) FunputDivider()
            content(item)
        }
    }
}

/**
 * Clips to and outlines the part of the card this row covers: the card's shape is laid over a
 * rectangle that runs past the row's open edges, so only the first row gets the top corners and
 * only the last the bottom ones, and the hairline stops where the next row carries it on.
 */
@Composable
private fun Modifier.cardSlice(isFirst: Boolean, isLast: Boolean): Modifier {
    val colors = FunputUi.colors
    val shape = FunputUi.shapes.card
    val strokeWidth = FunputUi.spacing.cardStrokeWidth
    return drawWithCache {
        val overhang = SliceOverhang.toPx()
        val top = if (isFirst) 0f else -overhang
        val bottom = if (isLast) size.height else size.height + overhang
        val outline = shape.createOutline(Size(size.width, bottom - top), layoutDirection, this)
        val path = Path().apply {
            addOutline(outline)
            translate(Offset(0f, top))
        }
        // Centred on the edge and clipped to the inside, so it lands where FunputCard's border does.
        val stroke = Stroke(width = strokeWidth.toPx() * 2f)
        onDrawWithContent {
            clipRect {
                clipPath(path) {
                    drawRect(colors.cardBackground)
                    this@onDrawWithContent.drawContent()
                    drawPath(path, colors.cardStroke, style = stroke)
                }
            }
        }
    }
}

/** Longer than any card corner, so an open edge never shows the start of a curve. */
private val SliceOverhang = 64.dp
