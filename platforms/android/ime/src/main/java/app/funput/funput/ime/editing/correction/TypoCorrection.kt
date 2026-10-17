package app.funput.funput.ime.editing.correction

import app.funput.funput.ime.editing.correction.report.KeyboardTouchSpread
import app.funput.funput.ime.nativebridge.correction.CorrectionVerdict
import app.funput.funput.ime.nativebridge.correction.TypoCorrectionEngine
import app.funput.funput.ime.suggestions.correction.CorrectionDictionary
import app.funput.funput.keyboard.touch.KeyTouchEvidence

/**
 * One document's typo-correction state: what the engine parked or applied last, and the
 * gates every decision passes. The decisions themselves live beside it: the boundary in
 * [BoundaryCorrection], Backspace in `undo/`, the bar in `preview/`.
 */
internal class TypoCorrection(
    val engine: TypoCorrectionEngine,
    private val enabled: () -> Boolean = { false },
    private val allowsField: () -> Boolean = { true },
    private val allowsLearning: () -> Boolean = { true },
) {
    /** The user's words and the shipped lists; with none, nothing is ever corrected. */
    var dictionary: CorrectionDictionary? = null

    /** The candidate the user tapped on the bar, for the Space that follows to apply. */
    var forced: String? = null
    var parked: ParkedCorrection? = null
    var applied: AppliedCorrection? = null
    val declines = CorrectionDeclines()
    val spread = KeyboardTouchSpread()
    private val boundary = BoundaryCorrection(this)

    val isOn: Boolean get() = enabled()
    val fieldAllows: Boolean get() = allowsField()

    /** A key is about to reach the engine: hand it the touch, and spend last word's state. */
    fun keyTyped(touch: KeyTouchEvidence?) {
        applied = null
        parked = null
        if (touch == null || !isOn) return
        engine.setNextKeyTouch(touch)
        spread.record(touch.typedDistance)
    }

    /** What the boundary writes: the engine's [written] output, or the repair in its place. */
    fun atBoundary(previous: String, written: String?, text: String): String? =
        boundary.resolve(previous, written, text).also { result ->
            val word = (result ?: previous + text).removeSuffix(text)
            if (isOn && allowsLearning() && word.isNotEmpty()) dictionary?.learn(word)
        }

    /** The engine's `uses` for [candidates]: shipped tier plus the user's own count. */
    fun uses(candidates: List<String>): IntArray {
        val dictionary = dictionary ?: return IntArray(candidates.size)
        return IntArray(candidates.size) { dictionary.weight(candidates[it]) }
    }

    /**
     * What the bar offers for [candidates]: the best, plus the runner-up when the two are too
     * close to call. Null when the host allows none of them.
     */
    fun rankedTexts(candidates: List<String>, uses: IntArray): List<String>? {
        val ranking = engine.rank(uses)
        if (ranking.verdict == CorrectionVerdict.REFUSED) return null
        val best = ranking.best?.let(candidates::getOrNull) ?: return null
        val second = ranking.second.takeIf { ranking.verdict == CorrectionVerdict.AMBIGUOUS }
        return listOfNotNull(best, second?.let(candidates::getOrNull))
    }

    /** Forget the last word; the engine is being cleared alongside. */
    fun reset() {
        forced = null
        parked = null
        applied = null
    }
}
