package app.funput.funput.ime.editing.correction.preview

import app.funput.funput.ime.editing.correction.TypoCorrection

/**
 * Works out what the bar should show after each key, the way iOS `computeCorrectionPreview`
 * does: an armed undo first, then a parked repair, then a preview of the word being typed.
 *
 * The preview is asked only for a word the engine already calls dead and the word source
 * does not know, so a live word costs nothing; the boundary reuses what the preview found.
 */
internal class CorrectionPreviewer(private val state: TypoCorrection) {
    fun compute(composing: String): CorrectionPreview? {
        val engine = state.engine
        state.applied?.let { applied ->
            if (engine.hasUndo()) return CorrectionPreview.Applied(applied.typed)
            state.applied = null
        }
        state.parked?.let { parked ->
            if (engine.hasPending()) return CorrectionPreview.Parked(parked.shown, parked.candidates)
            state.parked = null
        }
        if (!state.isOn || !state.fieldAllows || composing.isEmpty()) return null
        val dictionary = state.dictionary ?: return null
        if (dictionary.recognizes(composing)) return null
        if (engine.preview() == 0) return null
        val candidates = engine.candidates()
        val texts = state.rankedTexts(candidates, state.uses(candidates)) ?: return null
        return CorrectionPreview.Offered(composing, texts)
    }
}
