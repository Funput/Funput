package app.funput.funput.ime.speech.integration.actions

/** Consume both halves of Back; all other strokes invalidate before any hardware forwarding. */
internal class SpeechHardwareBoundary(private val back: () -> Boolean, private val invalidate: () -> Unit) {
    private var backConsumed = false

    fun down(isBack: Boolean): Boolean {
        if (isBack && (backConsumed || back())) {
            backConsumed = true
            return true
        }
        invalidate()
        return false
    }

    fun up(isBack: Boolean): Boolean {
        if (isBack && backConsumed) {
            backConsumed = false
            return true
        }
        invalidate()
        return false
    }
}
