package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeySwipeAction
import kotlin.math.abs

/** Recognizes deliberate horizontal swipes that begin and end on the same key. */
internal class KeySwipeGestureTracker(private val thresholdPx: Float) {
    private val startsByPointerId = mutableMapOf<Int, SwipeStart>()

    init {
        require(thresholdPx > 0f) { "Swipe threshold must be positive" }
    }

    fun start(pointerId: Int, key: KeySpec?, x: Float, y: Float) {
        val action = key?.horizontalSwipeAction
        if (action == null) {
            startsByPointerId.remove(pointerId)
        } else {
            startsByPointerId[pointerId] = SwipeStart(key.id, action, x, y)
        }
    }

    fun finish(pointerId: Int, key: KeySpec?, x: Float, y: Float): KeySwipeAction? {
        val start = startsByPointerId.remove(pointerId) ?: return null
        if (key?.id != start.keyId) return null

        val horizontalDistance = abs(x - start.x)
        val verticalDistance = abs(y - start.y)
        return if (
            horizontalDistance >= thresholdPx &&
            horizontalDistance > verticalDistance * HorizontalDominanceRatio
        ) {
            start.action
        } else {
            null
        }
    }

    fun cancel() = startsByPointerId.clear()

    private data class SwipeStart(
        val keyId: String,
        val action: KeySwipeAction,
        val x: Float,
        val y: Float,
    )

    companion object {
        private const val ThresholdDp = 32f
        private const val HorizontalDominanceRatio = 1.25f

        fun fromDensity(density: Float): KeySwipeGestureTracker {
            require(density > 0f) { "Density must be positive" }
            return KeySwipeGestureTracker(ThresholdDp * density)
        }
    }
}
