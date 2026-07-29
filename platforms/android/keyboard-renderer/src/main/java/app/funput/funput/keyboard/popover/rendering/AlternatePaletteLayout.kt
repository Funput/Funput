package app.funput.funput.keyboard.popover.rendering

import app.funput.funput.keyboard.layout.KeyBounds
import kotlin.math.ceil

internal data class AlternatePaletteLayout(
    val bounds: KeyBounds,
    val itemBounds: List<KeyBounds>,
    val sourceBounds: KeyBounds,
) {
    fun indexAt(x: Float, y: Float, density: Float): Int? {
        if (sourceBounds.expanded(8f * density).contains(x, y)) return 0
        return itemBounds.indexOfFirst { it.expanded(density).contains(x, y) }.takeIf { it >= 0 }
    }

    companion object {
        fun resolve(
            count: Int,
            source: KeyBounds,
            surface: KeyBounds,
            density: Float,
        ): AlternatePaletteLayout {
            val safe = surface.inset(6f * density, 4f * density)
            val padding = 6f * density
            val gap = 2f * density
            val targetWidth = 40f * density
            val cellHeight = 42f * density
            val available = (safe.width - padding * 2f).coerceAtLeast(1f)
            val columns = minOf(count, 9, ((available + gap) / targetWidth).toInt().coerceAtLeast(1))
            val rows = ceil(count.toDouble() / columns).toInt()
            val width = minOf(safe.width, columns * targetWidth - gap + padding * 2f)
            val height = rows * cellHeight + (rows - 1) * gap + padding * 2f
            val left = (source.centerX - width / 2f).coerceIn(safe.left, safe.right - width)
            val above = source.top - height - 4f * density
            val below = source.bottom + 4f * density
            val top = when {
                above >= safe.top -> above
                below + height <= safe.bottom -> below
                else -> maxOf(safe.top, minOf(above, safe.bottom - height))
            }
            val bounds = KeyBounds(left, top, left + width, top + height)
            return AlternatePaletteLayout(bounds, itemBounds(count, bounds, columns, padding, gap, cellHeight), source)
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
