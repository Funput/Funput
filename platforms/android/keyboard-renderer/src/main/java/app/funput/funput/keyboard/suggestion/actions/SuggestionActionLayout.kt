package app.funput.funput.keyboard.suggestion.actions

import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.suggestion.SuggestionBand
import app.funput.funput.keyboard.suggestion.SuggestionBandSegment
import app.funput.funput.keyboard.suggestion.SuggestionBandSegment.Tone

/**
 * The row the suggestion band turns into while a word's actions are open: the word, quoted
 * and muted so it reads as a label, then one slot per [SuggestionAction], then a close.
 */
internal object SuggestionActionLayout {
    const val WordTargetId = "suggestion-action-word"
    const val CloseTargetId = "suggestion-action-close"
    private const val ActionTargetPrefix = "suggestion-action-"
    private const val CloseTitle = "✕"

    // The action titles are longer than the word slot usually needs; close is an icon.
    private const val WordWeight = 1.3f
    private const val CloseWeight = 0.5f
    private val actionWeights = mapOf(SuggestionAction.BLOCK to 1.3f, SuggestionAction.FORGET to 0.9f)

    fun targetId(action: SuggestionAction): String = ActionTargetPrefix + action.name.lowercase()

    fun action(targetId: String?): SuggestionAction? =
        SuggestionAction.entries.firstOrNull { targetId(it) == targetId }

    fun owns(targetId: String?): Boolean =
        targetId == WordTargetId || targetId == CloseTargetId || action(targetId) != null

    fun segments(selection: SuggestionSelection, bounds: KeyBounds): List<SuggestionBandSegment> {
        val actions = SuggestionAction.entries
        val weights = listOf(WordWeight) + actions.map { actionWeights.getValue(it) } + CloseWeight
        val spans = SuggestionBand.spans(bounds, weights)
        val word = SuggestionBandSegment(
            WordTargetId, "“${selection.text}”", spans[0].start, spans[0].endInclusive,
            Tone.MUTED, pressable = false,
        )
        val slots = actions.mapIndexed { index, action ->
            val span = spans[index + 1]
            SuggestionBandSegment(targetId(action), action.title, span.start, span.endInclusive)
        }
        val close = spans.last().let { SuggestionBandSegment(CloseTargetId, CloseTitle, it.start, it.endInclusive) }
        return listOf(word) + slots + close
    }
}
