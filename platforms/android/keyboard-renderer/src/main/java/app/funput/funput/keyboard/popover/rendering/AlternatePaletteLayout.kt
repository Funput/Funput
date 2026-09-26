package app.funput.funput.keyboard.popover.rendering

import app.funput.funput.keyboard.layout.KeyBounds
import kotlin.math.ceil

internal data class AlternatePaletteLayout(
    val bounds: KeyBounds,
    val itemBounds: List<KeyBounds>,
    val sourceBounds: KeyBounds,
    val defaultIndex: Int?,
    /** True when a short viewport forces the palette to intersect its source key. */
    val overlapsSource: Boolean,
) {
    /**
     * The cell a moved finger is on. A palette clamped over its own key has no key region left
     * to stand for the default, so the touch's starting point holds it until the finger travels
     * away — otherwise a hand resting still would drift onto whichever cell happens to cover it.
     */
    fun selectionAt(x: Float, y: Float, startX: Float, startY: Float, density: Float): Int? {
        if (!overlapsSource) return indexAt(x, y, density)
        val dx = x - startX
        val dy = y - startY
        val slop = HoldSlop * density
        if (dx * dx + dy * dy <= slop * slop) return defaultIndex
        return indexAt(x, y, density)
    }

    fun indexAt(x: Float, y: Float, density: Float): Int? {
        if (!overlapsSource && sourceBounds.expanded(8f * density).contains(x, y)) {
            return defaultIndex
        }
        return itemBounds.indexOfFirst { it.expanded(density).contains(x, y) }.takeIf { it >= 0 }
    }

    companion object {
        /**
         * Preferred grid width. The palette stays narrow and wraps into more rows so it reads as
         * a block above the finger rather than a long strip across the keyboard.
         */
        private const val PreferredColumns = 6

        /** Past this the block would be taller than the keyboard, so wider rows win instead. */
        private const val MaximumRows = 3
        private const val PreferredSpan = 40f

        /** Cells shrink no further than this, even to win a row. */
        private const val MinimumSpan = 24f
        private const val CellHeight = 42f
        private const val Padding = 6f
        private const val Gap = 2f
        private const val SourceGap = 4f

        /**
         * How far the finger must leave its starting point before it stops meaning "the default
         * cell", used only when the palette covers the source key.
         */
        private const val HoldSlop = 16f

        fun resolve(
            count: Int,
            source: KeyBounds,
            surface: KeyBounds,
            density: Float,
            defaultIndex: Int? = 0,
            preferredColumns: Int? = null,
        ): AlternatePaletteLayout {
            require(count > 0) { "Palette must contain items" }
            require(defaultIndex == null || defaultIndex in 0 until count) { "Default alternate must be in bounds" }
            require(preferredColumns == null || preferredColumns > 0) {
                "Preferred columns must be positive"
            }
            val safe = surface.inset(6f * density, 4f * density)
            val padding = Padding * density
            val gap = Gap * density
            val cellHeight = CellHeight * density
            val available = (safe.width - padding * 2f).coerceAtLeast(1f)
            val columns = columnCount(count, available, density, preferredColumns ?: PreferredColumns)
            val rows = ceil(count.toDouble() / columns).toInt()
            val span = minOf(PreferredSpan * density, (available + gap) / columns)
            val width = minOf(safe.width, columns * span - gap + padding * 2f)
            val height = rows * cellHeight + (rows - 1) * gap + padding * 2f
            // Float rounding can invert the clamp range when the palette fills the safe width.
            val maxLeft = (safe.right - width).coerceAtLeast(safe.left)
            val left = (source.centerX - width / 2f).coerceIn(safe.left, maxLeft)
            // The surface may extend into screen space above the keyboard. Only overlap the
            // source when even that full viewport cannot fit the palette.
            val top = maxOf(safe.top, source.top - SourceGap * density - height)
            val bounds = KeyBounds(left, top, left + width, top + height)
            return AlternatePaletteLayout(
                bounds = bounds,
                itemBounds = itemBounds(count, bounds, columns, padding, gap, cellHeight),
                sourceBounds = source,
                defaultIndex = defaultIndex,
                overlapsSource = bounds.intersects(source),
            )
        }

        /**
         * Wraps at [preferredColumns] and widens only when the set would otherwise need more than
         * [MaximumRows] rows. The rows are then evened out, so thirteen cells read as 5 + 5 + 3
         * rather than 6 + 6 + 1.
         */
        private fun columnCount(count: Int, available: Float, density: Float, preferredColumns: Int): Int {
            val widthLimit = ((available + Gap * density) / (MinimumSpan * density))
                .toInt().coerceAtLeast(1)
            val wrapped = ceil(count.toDouble() / preferredColumns).toInt()
            val rows = minOf(MaximumRows, wrapped.coerceAtLeast(1))
            val balanced = ceil(count.toDouble() / rows).toInt()
            return minOf(count, minOf(widthLimit, balanced.coerceAtLeast(1)))
        }

        private fun itemBounds(
            count: Int,
            bounds: KeyBounds,
            columns: Int,
            padding: Float,
            gap: Float,
            height: Float,
        ): List<KeyBounds> {
            val width = (bounds.width - padding * 2f - (columns - 1) * gap) / columns
            return List(count) { index ->
                val row = index / columns
                val column = index % columns
                val left = bounds.left + padding + column * (width + gap)
                val top = bounds.top + padding + row * (height + gap)
                KeyBounds(left, top, left + width, top + height)
            }
        }
    }
}

private fun KeyBounds.inset(dx: Float, dy: Float) =
    KeyBounds(left + dx, top + dy, right - dx, bottom - dy)

private fun KeyBounds.expanded(value: Float) =
    KeyBounds(left - value, top - value, right + value, bottom + value)

private fun KeyBounds.intersects(other: KeyBounds) =
    left < other.right && other.left < right && top < other.bottom && other.top < bottom
