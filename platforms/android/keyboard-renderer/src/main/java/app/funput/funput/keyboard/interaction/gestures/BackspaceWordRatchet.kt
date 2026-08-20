package app.funput.funput.keyboard.interaction.gestures

import kotlin.math.abs
import kotlin.math.max

/**
 * Recognizes a leftward rub on the Backspace key and meters it into whole words.
 *
 * Claims as early as [activation] — inside the keycap — rather than at a swipe-sized
 * threshold. The contact has to be detached before the finger can drift onto a
 * neighbouring key, otherwise the touch pipeline commits that key on release.
 */
internal class BackspaceWordRatchet(
    private val activation: Float = DefaultActivation,
    private val dominance: Float = DefaultDominance,
    private val step: Float = DefaultStep,
) {
    private var emittedSteps = 0

    init {
        require(activation > 0f) { "Word-delete activation must be positive" }
        require(step > 0f) { "Word-delete step must be positive" }
    }

    /** Whether this contact has become a word-delete rub. */
    fun shouldClaim(translationX: Float, translationY: Float): Boolean =
        translationX <= -activation && abs(translationX) > abs(translationY) * dominance

    /**
     * How many further words the finger has asked to delete since the last call.
     *
     * Rightward travel never rewinds the anchor and never returns a count, which is how
     * "swiping right does nothing" falls out without a special case.
     */
    fun update(translationX: Float): Int {
        val steps = (max(0f, -translationX) / step).toInt()
        val delta = steps - emittedSteps
        if (delta <= 0) return 0
        emittedSteps = steps
        return delta
    }

    /** True once the rub has deleted at least one word. */
    val hasDeleted: Boolean get() = emittedSteps > 0

    private companion object {
        const val DefaultActivation = 16f
        const val DefaultDominance = 1.25f
        const val DefaultStep = 40f
    }
}
