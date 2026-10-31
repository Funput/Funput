package app.funput.funput.keyboard.suggestion.actions

import app.funput.funput.keyboard.layout.ResolvedKeyboard
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.suggestion.SuggestionBand
import app.funput.funput.keyboard.suggestion.SuggestionBandSegment
import app.funput.funput.keyboard.suggestion.SuggestionChip

/**
 * The actions a long press opens over the suggestion band, in place of the candidates.
 *
 * It stays open only over the candidates it opened on: once the bar shows another list the
 * word may be gone, so the strip quietly gives way. Typing any key closes it as well.
 */
internal class SuggestionActionStrip(
    private val suggestions: () -> List<SuggestionChip>,
    private val onAction: (SuggestionSelection, SuggestionAction) -> Unit,
    private val onChanged: () -> Unit,
) {
    private var selection: SuggestionSelection? = null
    private var openedOn: List<SuggestionChip> = emptyList()

    /** The word the strip acts on, or null while the band shows candidates. */
    val opened: SuggestionSelection? get() = selection?.takeIf { suggestions() == openedOn }

    fun open(selection: SuggestionSelection) {
        this.selection = selection
        openedOn = suggestions()
        onChanged()
    }

    fun close() {
        if (selection == null) return
        selection = null
        openedOn = emptyList()
        onChanged()
    }

    fun segments(keyboard: ResolvedKeyboard?): List<SuggestionBandSegment> {
        val selection = opened ?: return emptyList()
        val bounds = keyboard?.suggestionBar?.suggestionsBounds ?: return emptyList()
        return SuggestionActionLayout.segments(selection, bounds)
    }

    /** The strip's slot under a touch, where the candidates would otherwise be hit; keys still win. */
    fun targetAt(x: Float, y: Float, keyboard: ResolvedKeyboard?): String? {
        val bar = keyboard?.suggestionBar ?: return null
        if (opened == null || keyboard.keyAt(x, y) != null || !bar.suggestionsHitBounds.contains(x, y)) return null
        return SuggestionBand.targetAt(segments(keyboard), x)
    }

    /**
     * Acts on a released target. Returns true when the target was the strip's, so it must not
     * also reach the keyboard; any other key closes the strip and goes on as typed.
     */
    fun release(targetId: String?): Boolean {
        val selection = opened
        if (selection == null || !SuggestionActionLayout.owns(targetId)) {
            if (targetId != null || selection == null) close()
            return false
        }
        val action = SuggestionActionLayout.action(targetId)
        if (targetId != SuggestionActionLayout.WordTargetId) close()
        action?.let { onAction(selection, it) }
        return true
    }
}

