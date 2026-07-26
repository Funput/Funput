package app.funput.funput.keyboard.ui.emoji

import kotlin.math.floor

internal object EmojiGridMetrics {
    const val CellDp = 44
    const val HorizontalInsetDp = 8

    fun columnsFor(widthDp: Float): Int =
        floor((widthDp - HorizontalInsetDp * 2) / CellDp).toInt().coerceIn(7, 10)
}
