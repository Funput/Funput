package app.funput.funput.keyboard.ui.kaomoji.browser

internal object KaomojiCellMetrics {
    const val MinimumWidth = 44f
    const val HorizontalPadding = 16f

    fun widthFor(measuredTextWidth: Float, availableWidth: Float): Float =
        (measuredTextWidth + HorizontalPadding).coerceIn(
            MinimumWidth,
            availableWidth.coerceAtLeast(MinimumWidth),
        )
}
