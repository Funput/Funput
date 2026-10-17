package app.funput.funput.ime.suggestions.bar

import app.funput.funput.ime.editing.correction.preview.CorrectionPreview
import app.funput.funput.keyboard.suggestion.SuggestionChip
import app.funput.funput.keyboard.suggestion.SuggestionChipKind

/**
 * Lays out the bar's three slots: correction first, then the personal suggestions or
 * editor completions in what room is left, without repeating a word already shown.
 */
internal object SuggestionBarComposer {
    const val SlotCount = 3

    fun compose(correction: CorrectionPreview?, personal: List<String>): List<SuggestionChip> {
        val leading = when (correction) {
            is CorrectionPreview.Offered -> offer(correction.typed, correction.candidates)
            is CorrectionPreview.Parked -> offer(correction.typed, correction.candidates)
            is CorrectionPreview.Applied -> listOf(SuggestionChip(correction.typed, SuggestionChipKind.CORRECTION_UNDO))
            null -> emptyList()
        }
        val shown = leading.mapTo(HashSet()) { it.text }
        val rest = personal.filter { it !in shown }.map(::SuggestionChip)
        return (leading + rest).take(SlotCount)
    }

    private fun offer(typed: String, candidates: List<String>): List<SuggestionChip> =
        listOf(SuggestionChip(typed, SuggestionChipKind.CORRECTION_TYPED)) +
            candidates.take(SlotCount - 1).map { SuggestionChip(it, SuggestionChipKind.CORRECTION_CANDIDATE) }
}
