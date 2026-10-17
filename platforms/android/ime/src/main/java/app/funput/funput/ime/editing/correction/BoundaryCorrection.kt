package app.funput.funput.ime.editing.correction

/**
 * Answers a correction the engine parked at a word boundary, before anything is written:
 * the repair replaces the boundary's output, so the document only ever sees one write and
 * the mistyped word never reaches it. Mirrors iOS `applyPendingCorrection`.
 */
internal class BoundaryCorrection(private val state: TypoCorrection) {
    private sealed interface Choice {
        data class Apply(val index: Int) : Choice
        data class Park(val texts: List<String>) : Choice
        data object Decline : Choice
    }

    /**
     * [previous] is the composing word, [written] what the engine's boundary returned (null
     * when it only adds [text]). Returns what the session should write in their place.
     */
    fun resolve(previous: String, written: String?, text: String): String? {
        val forced = state.forced
        state.reset()
        val engine = state.engine
        if (!state.isOn || !engine.hasPending()) return written
        val document = written ?: previous + text
        val backspace = engine.pendingBackspace()
        val candidates = engine.candidates()
        if (backspace < 2 || backspace > document.scalarCount() || candidates.isEmpty()) {
            return decline(written)
        }
        val shown = document.lastScalars(backspace).dropLastScalars(1)
        return when (val choice = choose(forced, shown, previous, candidates)) {
            is Choice.Apply -> apply(choice.index, document, shown, previous) ?: written
            is Choice.Park -> written.also { state.parked = ParkedCorrection(shown, document, choice.texts) }
            Choice.Decline -> decline(written)
        }
    }

    private fun choose(forced: String?, shown: String, composed: String, candidates: List<String>): Choice {
        if (forced != null) return candidates.indexOf(forced).let { if (it >= 0) Choice.Apply(it) else Choice.Decline }
        if (!state.fieldAllows) {
            state.declines.fieldRefused += 1
            return Choice.Decline
        }
        val dictionary = state.dictionary ?: return Choice.Decline
        // The restored keys or the syllable built from them: either one known means meant.
        if (dictionary.recognizes(shown) || (composed.isNotEmpty() && dictionary.recognizes(composed))) {
            state.declines.recognized += 1
            return Choice.Decline
        }
        val uses = state.uses(candidates)
        state.engine.choose(uses)?.let { return Choice.Apply(it) }
        return state.rankedTexts(candidates, uses)?.let { Choice.Park(it) } ?: Choice.Decline
    }

    private fun apply(index: Int, document: String, shown: String, composed: String): String? {
        val edit = state.engine.apply(index) ?: return null
        state.applied = AppliedCorrection(typed = shown, composed = composed, corrected = edit.text)
        return document.dropLastScalars(edit.backspace) + edit.text
    }

    /** Declining is a no-op for the engine: the boundary already did what it always did. */
    private fun decline(written: String?): String? {
        state.engine.apply(null)
        return written
    }
}
