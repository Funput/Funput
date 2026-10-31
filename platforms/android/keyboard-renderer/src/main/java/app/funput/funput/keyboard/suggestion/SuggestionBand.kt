package app.funput.funput.keyboard.suggestion

import app.funput.funput.keyboard.interaction.SuggestionTargetIds
import app.funput.funput.keyboard.layout.KeyBounds

/**
 * One slot drawn on the suggestion band. The candidates and the action strip a long press
 * opens are both rows of these, so they share one painter and one hit test.
 */
internal data class SuggestionBandSegment(
    val targetId: String,
    val title: String,
    val left: Float,
    val right: Float,
    val tone: Tone = Tone.PLAIN,
    /** False for a slot that only labels the row, such as the word the strip acts on. */
    val pressable: Boolean = true,
) {
    enum class Tone { PLAIN, ACCENT, MUTED }

    val centerX: Float get() = (left + right) / 2f
    val width: Float get() = right - left
}

internal object SuggestionBand {
    /** The candidates in equal slots, the recommended one accented. */
    fun chips(suggestions: List<SuggestionChip>, bounds: KeyBounds): List<SuggestionBandSegment> {
        if (suggestions.isEmpty()) return emptyList()
        val accent = suggestions.accentIndex()
        val width = bounds.width / suggestions.size
        return suggestions.mapIndexed { index, chip ->
            SuggestionBandSegment(
                targetId = SuggestionTargetIds.id(index),
                title = chip.title,
                left = bounds.left + width * index,
                right = bounds.left + width * (index + 1),
                tone = if (index == accent) SuggestionBandSegment.Tone.ACCENT else SuggestionBandSegment.Tone.PLAIN,
            )
        }
    }

    /** Splits [bounds] left to right in proportion to [weights]. */
    fun spans(bounds: KeyBounds, weights: List<Float>): List<ClosedFloatingPointRange<Float>> {
        val total = weights.sum()
        if (total <= 0f) return emptyList()
        var left = bounds.left
        return weights.map { weight ->
            val right = left + bounds.width * weight / total
            (left..right).also { left = right }
        }
    }

    /** The slot under [x]; the outer slots take the slack either side of the band. */
    fun targetAt(segments: List<SuggestionBandSegment>, x: Float): String? =
        segments.firstOrNull { x < it.right }?.targetId ?: segments.lastOrNull()?.targetId
}
