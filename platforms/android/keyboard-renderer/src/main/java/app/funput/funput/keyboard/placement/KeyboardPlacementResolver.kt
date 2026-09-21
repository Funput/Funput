package app.funput.funput.keyboard.placement

import kotlin.math.roundToInt

/** Pure height policy shared by the IME host and its tests. */
object KeyboardPlacementResolver {
    const val MinimumAppFraction = 0.30f
    const val MinimumAppHeightDp = 160f
    const val MaximumElevatedOffsetDp = 160f

    fun resolve(
        preferences: KeyboardPlacementPreferences,
        density: Float,
        viewportHeightPx: Int,
        baseKeyboardHeightPx: Int,
    ): KeyboardPlacementResult {
        require(density > 0f)
        val minimumAppHeight = maxOf(
            (viewportHeightPx * MinimumAppFraction).roundToInt(),
            (MinimumAppHeightDp * density).roundToInt(),
        )
        val availableOffset = (viewportHeightPx - baseKeyboardHeightPx - minimumAppHeight)
            .coerceAtLeast(0)
        val maximumOffset = minOf(
            availableOffset,
            (MaximumElevatedOffsetDp * density).roundToInt(),
        )
        val requestedOffset = (preferences.elevatedOffsetDp * density).roundToInt()
        val appliedOffset = if (preferences.activeMode == KeyboardPlacementMode.ELEVATED) {
            requestedOffset.coerceAtMost(maximumOffset)
        } else {
            0
        }
        return KeyboardPlacementResult(appliedOffset, maximumOffset)
    }

    fun resolveHorizontal(
        preferences: KeyboardPlacementPreferences,
        viewportWidthPx: Int,
    ): KeyboardHorizontalPlacementResult {
        val width = if (preferences.activeMode == KeyboardPlacementMode.ONE_HANDED) {
            (viewportWidthPx * preferences.oneHandedWidthFraction).roundToInt()
        } else {
            viewportWidthPx
        }.coerceIn(0, viewportWidthPx)
        val start = if (
            preferences.activeMode == KeyboardPlacementMode.ONE_HANDED &&
            preferences.oneHandedSide == OneHandedSide.RIGHT
        ) viewportWidthPx - width else 0
        return KeyboardHorizontalPlacementResult(width, start)
    }
}

data class KeyboardPlacementResult(
    val appliedOffsetPx: Int,
    val maximumOffsetPx: Int,
)

data class KeyboardHorizontalPlacementResult(
    val contentWidthPx: Int,
    val contentStartPx: Int,
)
