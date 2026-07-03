package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyboardLayout

/** Resolves relative key weights into pixel bounds for the current surface. */
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

        val contentWidth = width - spec.horizontalPadding * 2f
        val contentHeight = height - spec.verticalPadding * 2f -
            spec.suggestionBarHeight - spec.suggestionBarGap
        val rowHeight = (contentHeight - spec.verticalGap * (layout.rows.size - 1)) /
            layout.rows.size
        require(contentWidth > 0f && rowHeight > 0f) {
            "Keyboard size is too small for its geometry"
        }

        val canonicalUnit = (contentWidth - spec.horizontalGap * (CanonicalColumnCount - 1)) /
            CanonicalColumnCount
        val suggestionBar = resolveSuggestionBar(layout, width, spec)
        val rowsTop = suggestionBar.bounds.bottom + spec.suggestionBarGap
        val rows = layout.rows.mapIndexed { rowIndex, row ->
            val rowTop = rowsTop + rowIndex * (rowHeight + spec.verticalGap)
            val inset = canonicalUnit * row.horizontalInsetUnits
            val rowWidth = contentWidth - inset * 2f
            val totalWeight = row.keys.sumOf { key -> key.widthWeight.toDouble() }.toFloat()
            val weightedWidth = rowWidth - spec.horizontalGap * (row.keys.size - 1)
            val widthPerWeight = weightedWidth / totalWeight
            require(widthPerWeight > 0f) { "Keyboard row is too narrow for its keys" }

            var cursor = spec.horizontalPadding + inset
            row.keys.map { key ->
                val keyWidth = widthPerWeight * key.widthWeight
                ResolvedKey(
                    spec = key,
                    bounds = KeyBounds(cursor, rowTop, cursor + keyWidth, rowTop + rowHeight),
                ).also { cursor += keyWidth + spec.horizontalGap }
            }
        }
        return ResolvedKeyboard(width, height, suggestionBar, rows)
    }

    private fun resolveSuggestionBar(
        layout: KeyboardLayout,
        width: Float,
        spec: KeyboardGeometrySpec,
    ): ResolvedSuggestionBar {
        val top = spec.verticalPadding
        val bottom = top + spec.suggestionBarHeight
        val left = spec.horizontalPadding
        val right = width - spec.horizontalPadding
        val emojiLeft = right - spec.suggestionBarHeight
        val suggestionBar = ResolvedSuggestionBar(
            bounds = KeyBounds(left, top, right, bottom),
            suggestionsBounds = KeyBounds(left, top, emojiLeft - spec.horizontalGap, bottom),
            emojiKey = ResolvedKey(
                layout.suggestionBar.emojiKey,
                KeyBounds(emojiLeft, top, right, bottom),
            ),
        )
        require(suggestionBar.suggestionsBounds.width > 0f) {
            "Keyboard is too narrow for the suggestion bar"
        }
        return suggestionBar
    }
}
