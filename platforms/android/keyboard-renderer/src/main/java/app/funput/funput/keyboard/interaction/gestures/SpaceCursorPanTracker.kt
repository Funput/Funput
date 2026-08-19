package app.funput.funput.keyboard.interaction.gestures

/**
 * Converts horizontal travel on the spacebar into whole-character caret steps.
 *
 * Keeps the sub-step remainder so a slow drag advances exactly once per [stepWidth]
 * instead of drifting, and so reversing direction costs nothing.
 */
internal class SpaceCursorPanTracker(private val stepWidth: Float = DefaultStepWidth) {
    private var emittedSteps = 0

    init {
        require(stepWidth > 0f) { "Trackpad step width must be positive" }
    }

    /**
     * @param translationX total travel since the pan began, not since the last call.
     * @return signed character offset to apply now; zero when the finger has not
     *   crossed into a new step.
     */
    fun update(translationX: Float): Int {
        val steps = (translationX / stepWidth).toInt()
        val delta = steps - emittedSteps
        if (delta == 0) return 0
        emittedSteps = steps
        return delta
    }

    private companion object {
        const val DefaultStepWidth = 10f
    }
}
