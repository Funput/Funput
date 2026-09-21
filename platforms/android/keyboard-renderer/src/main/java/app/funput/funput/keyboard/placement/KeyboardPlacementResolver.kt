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
}

data class KeyboardPlacementResult(
    val appliedOffsetPx: Int,
    val maximumOffsetPx: Int,
)
