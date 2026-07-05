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
        require(contentWidth > 0f && contentHeight > 0f) {
            "Keyboard size is too small for its geometry"
        }

        val horizontalMetrics = resolveHorizontalMetrics(contentWidth, spec)
        val verticalMetrics = resolveVerticalMetrics(
            contentHeight = contentHeight,
            rowCount = layout.rows.size,
            canonicalUnit = horizontalMetrics.canonicalUnit,
            spec = spec,
        )
        val resolvedSpec = spec.copy(
            horizontalGap = horizontalMetrics.horizontalGap,
            verticalGap = verticalMetrics.verticalGap,
        )

        val suggestionBar = ToolbarGeometry.resolve(layout, width, resolvedSpec)
        val rowsTop = suggestionBar?.bounds?.bottom?.plus(resolvedSpec.suggestionBarGap)
            ?: spec.verticalPadding
        val rowsTopOffset = rowsTop
        val rows = layout.rows.mapIndexed { rowIndex, row ->
            val rowTop = rowsTopOffset + rowIndex * (verticalMetrics.rowHeight + resolvedSpec.verticalGap)
            val inset = horizontalMetrics.canonicalUnit * row.horizontalInsetUnits
            val rowWidth = contentWidth - inset * 2f
            val totalWeight = row.keys.sumOf { key -> key.widthWeight.toDouble() }.toFloat()
            val weightedWidth = rowWidth - resolvedSpec.horizontalGap * (row.keys.size - 1)
            val widthPerWeight = weightedWidth / totalWeight
            require(widthPerWeight > 0f) { "Keyboard row is too narrow for its keys" }

            var cursor = spec.horizontalPadding + inset
            row.keys.map { key ->
                val keyWidth = widthPerWeight * key.widthWeight
                ResolvedKey(
                    spec = key,
                    bounds = KeyBounds(cursor, rowTop, cursor + keyWidth, rowTop + verticalMetrics.rowHeight),
                ).also { cursor += keyWidth + resolvedSpec.horizontalGap }
            }
        }
        return KeyboardHitTargetResolver.resolve(
            ResolvedKeyboard(width, height, suggestionBar, rows),
        )
    }

    private data class HorizontalMetrics(
        val canonicalUnit: Float,
        val horizontalGap: Float,
    )

    private data class VerticalMetrics(
        val rowHeight: Float,
        val verticalGap: Float,
    )

    private fun resolveHorizontalMetrics(
        contentWidth: Float,
        spec: KeyboardGeometrySpec,
    ): HorizontalMetrics {
        val canonicalUnit = contentWidth /
            (CanonicalColumnCount + (CanonicalColumnCount - 1) * spec.horizontalGapRatio)
        val horizontalGap = canonicalUnit * spec.horizontalGapRatio
        return HorizontalMetrics(canonicalUnit, horizontalGap)
    }

    private fun resolveVerticalMetrics(
        contentHeight: Float,
        rowCount: Int,
        canonicalUnit: Float,
        spec: KeyboardGeometrySpec,
    ): VerticalMetrics {
        require(rowCount > 0) { "Keyboard layout must contain at least one row" }

        val maxRowHeight = canonicalUnit / spec.keyAspectRatio * spec.heightScale
        val splitDenominator = rowCount + (rowCount - 1) * spec.verticalGapRatio
        val splitRowHeight = contentHeight / splitDenominator
        val rowHeight = minOf(splitRowHeight, maxRowHeight)
        val verticalGap = rowHeight * spec.verticalGapRatio
        val rowsBlockHeight = rowCount * rowHeight + (rowCount - 1) * verticalGap
        require(rowsBlockHeight <= contentHeight + 0.5f) {
            "Keyboard height is too small for its row geometry"
        }
        return VerticalMetrics(
            rowHeight = rowHeight,
            verticalGap = verticalGap,
        )
    }

}
