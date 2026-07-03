package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardLayout

data class KeyBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
}

data class ResolvedKey(
    val spec: KeySpec,
    val bounds: KeyBounds,
)

data class ResolvedSuggestionBar(
    val bounds: KeyBounds,
    val suggestionsBounds: KeyBounds,
    val emojiKey: ResolvedKey,
)

data class ResolvedKeyboard(
    val width: Float,
    val height: Float,
    val suggestionBar: ResolvedSuggestionBar,
    val rows: List<List<ResolvedKey>>,
) {
    val keys: List<ResolvedKey> = buildList {
        add(suggestionBar.emojiKey)
        rows.forEach(::addAll)
    }

    fun keyAt(x: Float, y: Float): ResolvedKey? = keys.firstOrNull { key -> key.bounds.contains(x, y) }
}

data class KeyboardGeometrySpec(
    val horizontalPadding: Float,
    val verticalPadding: Float,
    val horizontalGap: Float,
    val verticalGap: Float,
    val suggestionBarHeight: Float,
    val suggestionBarGap: Float,
) {
    init {
        require(horizontalPadding >= 0f) { "Horizontal padding must not be negative" }
        require(verticalPadding >= 0f) { "Vertical padding must not be negative" }
        require(horizontalGap >= 0f) { "Horizontal gap must not be negative" }
        require(verticalGap >= 0f) { "Vertical gap must not be negative" }
        require(suggestionBarHeight > 0f) { "Suggestion bar height must be positive" }
        require(suggestionBarGap >= 0f) { "Suggestion bar gap must not be negative" }
    }

    companion object {
        fun fromDensity(density: Float): KeyboardGeometrySpec {
            require(density > 0f) { "Density must be positive" }
            return KeyboardGeometrySpec(
                horizontalPadding = 7f * density,
                verticalPadding = 8f * density,
                horizontalGap = 5f * density,
                verticalGap = 6f * density,
                suggestionBarHeight = 42f * density,
                suggestionBarGap = 6f * density,
            )
        }
    }
}

object KeyboardGeometry {
    private const val CanonicalColumnCount = 10

    fun resolve(
        layout: KeyboardLayout,
        width: Float,
        height: Float,
        spec: KeyboardGeometrySpec,
    ): ResolvedKeyboard {
        require(width > 0f) { "Keyboard width must be positive" }
        require(height > 0f) { "Keyboard height must be positive" }

        val contentWidth = width - (spec.horizontalPadding * 2f)
        val contentHeight = height -
            (spec.verticalPadding * 2f) -
            spec.suggestionBarHeight -
            spec.suggestionBarGap
        val rowHeight = (
            contentHeight - spec.verticalGap * (layout.rows.size - 1)
            ) / layout.rows.size

        require(contentWidth > 0f && rowHeight > 0f) { "Keyboard size is too small for its geometry" }

        val canonicalUnit = (
            contentWidth - spec.horizontalGap * (CanonicalColumnCount - 1)
            ) / CanonicalColumnCount

        val suggestionTop = spec.verticalPadding
        val suggestionBottom = suggestionTop + spec.suggestionBarHeight
        val contentLeft = spec.horizontalPadding
        val contentRight = width - spec.horizontalPadding
        val emojiLeft = contentRight - spec.suggestionBarHeight
        val suggestionBar = ResolvedSuggestionBar(
            bounds = KeyBounds(contentLeft, suggestionTop, contentRight, suggestionBottom),
            suggestionsBounds = KeyBounds(
                left = contentLeft,
                top = suggestionTop,
                right = emojiLeft - spec.horizontalGap,
                bottom = suggestionBottom,
            ),
            emojiKey = ResolvedKey(
                spec = layout.suggestionBar.emojiKey,
                bounds = KeyBounds(emojiLeft, suggestionTop, contentRight, suggestionBottom),
            ),
        )
        require(suggestionBar.suggestionsBounds.width > 0f) { "Keyboard is too narrow for the suggestion bar" }

        val rowsTop = suggestionBottom + spec.suggestionBarGap

        val rows = layout.rows.mapIndexed { rowIndex, row ->
            val rowTop = rowsTop + rowIndex * (rowHeight + spec.verticalGap)
            val inset = canonicalUnit * row.horizontalInsetUnits
            val rowLeft = spec.horizontalPadding + inset
            val rowWidth = contentWidth - (inset * 2f)
            val totalWeight = row.keys.sumOf { key -> key.widthWeight.toDouble() }.toFloat()
            val weightedWidth = rowWidth - spec.horizontalGap * (row.keys.size - 1)
            val widthPerWeight = weightedWidth / totalWeight

            require(widthPerWeight > 0f) { "Keyboard row is too narrow for its keys" }

            var cursor = rowLeft
            row.keys.map { key ->
                val keyWidth = widthPerWeight * key.widthWeight
                ResolvedKey(
                    spec = key,
                    bounds = KeyBounds(
                        left = cursor,
                        top = rowTop,
                        right = cursor + keyWidth,
                        bottom = rowTop + rowHeight,
                    ),
                ).also {
                    cursor += keyWidth + spec.horizontalGap
                }
            }
        }

        return ResolvedKeyboard(
            width = width,
            height = height,
            suggestionBar = suggestionBar,
            rows = rows,
        )
    }
}
