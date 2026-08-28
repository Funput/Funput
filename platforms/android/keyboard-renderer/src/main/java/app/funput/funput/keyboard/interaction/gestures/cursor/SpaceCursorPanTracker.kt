package app.funput.funput.keyboard.interaction.gestures.cursor

/**
 * Converts travel on the spacebar into whole caret steps on both axes.
 *
 * Keeps the sub-step remainder per axis so a slow drag advances exactly once per step instead of
 * drifting, and so reversing direction costs nothing.
 *
 * Vertical steps are the coarser of the two on purpose: a column is one character, while a line is
 * a whole paragraph jump the user has to read to confirm.
 */
internal class SpaceCursorPanTracker(
    private val stepWidth: Float = DefaultStepWidth,
    private val stepHeight: Float = DefaultStepHeight,
) {
    private var emittedColumns = 0
    private var emittedLines = 0

    init {
        require(stepWidth > 0f) { "Trackpad step width must be positive" }
        require(stepHeight > 0f) { "Trackpad step height must be positive" }
    }

    /**
     * @param translationX total sideways travel since the pan began, not since the last call.
     * @param translationY total vertical travel since the pan began, not since the last call.
     * @return the caret step to apply now; empty when the finger has not crossed into a new step
     *   on either axis.
     */
    fun update(translationX: Float, translationY: Float): CursorPanStep {
        val columns = (translationX / stepWidth).toInt()
        val lines = (translationY / stepHeight).toInt()
        val step = CursorPanStep(
            columns = columns - emittedColumns,
            lines = lines - emittedLines,
        )
        emittedColumns = columns
        emittedLines = lines
        return step
    }

    private companion object {
        const val DefaultStepWidth = 10f
        const val DefaultStepHeight = 24f
    }
}
