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
        val hasSuggestionBar = layout.suggestionBar != null
        val barHeight = if (hasSuggestionBar) spec.suggestionBarHeight else 0f
        val barGap = if (hasSuggestionBar) spec.suggestionBarGap else 0f
        val contentHeight = height - spec.verticalPadding * 2f - barHeight - barGap
        val rowHeight = (contentHeight - spec.verticalGap * (layout.rows.size - 1)) /
            layout.rows.size
        require(contentWidth > 0f && rowHeight > 0f) {
            "Keyboard size is too small for its geometry"
        }

        val canonicalUnit = (contentWidth - spec.horizontalGap * (CanonicalColumnCount - 1)) /
            CanonicalColumnCount
        val suggestionBar = resolveSuggestionBar(layout, width, spec)
        val rowsTop = suggestionBar?.bounds?.bottom?.plus(spec.suggestionBarGap) ?: spec.verticalPadding
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
        return KeyboardHitTargetResolver.resolve(
            ResolvedKeyboard(width, height, suggestionBar, rows),
        )
    }

    private fun resolveSuggestionBar(
        layout: KeyboardLayout,
        width: Float,
        spec: KeyboardGeometrySpec,
    ): ResolvedSuggestionBar? {
        val barSpec = layout.suggestionBar ?: return null
        val top = spec.verticalPadding
        val bottom = top + spec.suggestionBarHeight
        val left = spec.horizontalPadding
        val right = width - spec.horizontalPadding
        val emojiLeft = right - spec.suggestionBarHeight
        val suggestionBar = ResolvedSuggestionBar(
            bounds = KeyBounds(left, top, right, bottom),
            suggestionsBounds = KeyBounds(left, top, emojiLeft - spec.horizontalGap, bottom),
            emojiKey = ResolvedKey(
                barSpec.emojiKey,
                KeyBounds(emojiLeft, top, right, bottom),
            ),
        )
        require(suggestionBar.suggestionsBounds.width > 0f) {
            "Keyboard is too narrow for the suggestion bar"
        }
        return suggestionBar
    }
}
