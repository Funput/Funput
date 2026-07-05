package app.funput.funput.keyboard.ui

import kotlin.math.roundToInt

internal object EmojiGridColumns {
    /**
     * Emoji size equals the cell edge (panel width / columns), so columns are derived
     * from a target cell width. [TargetCellDp] is kept close to the denser grids used
     * by system keyboards; the clamp avoids oversized emojis on narrow screens and an
     * overly cramped grid on tablets.
     */
    private const val TargetCellDp = 34f
    private const val MinColumns = 9
    private const val MaxColumns = 15

    fun forWidth(widthDp: Float): Int =
        (widthDp / TargetCellDp).roundToInt().coerceIn(MinColumns, MaxColumns)
}
