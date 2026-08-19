package app.funput.funput.ime.editing.gestures

/**
 * Recognizes two space presses in quick succession.
 *
 * A value type over an injected clock, so the double-tap window is testable without
 * waiting on wall time.
 */
internal class SpaceTapTracker(
    private val doubleTapIntervalMillis: Long = DefaultIntervalMillis,
    private val clock: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private var lastTapTime: Long? = null

    init {
        require(doubleTapIntervalMillis > 0L) { "Double-tap interval must be positive" }
    }

    /**
     * Records a space press and reports whether it completes a double tap.
     *
     * A firing press clears the sequence, so three spaces produce one substitution
     * and a plain space rather than a second one.
     */
    fun registerSpace(): Boolean {
        val tapTime = clock()
        val last = lastTapTime
        if (last != null && tapTime - last <= doubleTapIntervalMillis) {
            reset()
            return true
        }
        lastTapTime = tapTime
        return false
    }

    /**
     * Called for every non-space key: two spaces separated by other input are not a
     * double tap, however fast they arrive.
     */
    fun reset() {
        lastTapTime = null
    }

    private companion object {
        const val DefaultIntervalMillis = 300L
    }
}
