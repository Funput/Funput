package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.KeyboardClipboardOffer
import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.suggestion.ClipboardOfferChip
import app.funput.funput.keyboard.suggestion.SuggestionChip

internal object SuggestionTargetIds {
    private val ids = arrayOf("suggestion-0", "suggestion-1", "suggestion-2")

    fun id(index: Int): String = ids[index]

    fun indexOf(targetId: String): Int? {
        val index = ids.indexOf(targetId)
        return index.takeIf { it >= 0 }
    }
}

/**
 * The key, candidate or paste chip under a touch.
 *
 * [clipboardOffer] and [density] only matter for a touch on a band with no candidates, where the
 * paste chip may be showing; a touch on a key never evaluates them.
 */
internal fun ResolvedKeyboard.interactionTargetAt(
    x: Float,
    y: Float,
    suggestionCount: Int,
    clipboardOffer: KeyboardClipboardOffer? = null,
    density: Float = 1f,
): String? {
    keyAt(x, y)?.let { return it.spec.id }
    val bar = suggestionBar ?: return null
    if (!bar.suggestionsHitBounds.contains(x, y)) return null
    if (suggestionCount <= 0) {
        // The whole band takes the tap, as it does for candidates: the chip is the only thing on it.
        val chip = ClipboardOfferChip.isVisible(clipboardOffer, suggestionCount, bar, density)
        return ClipboardOfferChip.TargetId.takeIf { chip }
    }

    // Segments follow what is drawn, not the wider hit region: a tap in the slack around
    // the band belongs to the nearest candidate instead of shifting every boundary.
    val bounds = bar.suggestionsBounds
    val segmentWidth = bounds.width / suggestionCount
    val index = ((x - bounds.left) / segmentWidth).toInt().coerceIn(0, suggestionCount - 1)
    return SuggestionTargetIds.id(index)
}

internal fun List<SuggestionChip>.selectionForTarget(targetId: String): SuggestionSelection? {
    val index = SuggestionTargetIds.indexOf(targetId) ?: return null
    val chip = getOrNull(index) ?: return null
    return SuggestionSelection(index, chip.text, chip.kind)
}
