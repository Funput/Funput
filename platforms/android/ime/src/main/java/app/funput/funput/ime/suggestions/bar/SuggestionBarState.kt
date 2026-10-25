package app.funput.funput.ime.suggestions.bar

import app.funput.funput.ime.editing.correction.preview.CorrectionPreview
import app.funput.funput.keyboard.model.SuggestionSelection
import app.funput.funput.keyboard.suggestion.SuggestionChip

/**
 * The two things the bar draws from, kept apart so each source keeps its own list: the
 * personal suggestions (or the editor's completions) and the correction preview.
 */
internal class SuggestionBarState(private val show: (List<SuggestionChip>) -> Unit) {
    private var personal: List<String> = emptyList()
    private var correction: CorrectionPreview? = null
    private var shortcutUndo: String? = null
    private var shown: List<SuggestionChip>? = null

    /** The personal suggestion service's or the editor's latest words. */
    fun showPersonal(values: List<String>) {
        personal = values
        publish()
    }

    fun showCorrection(value: CorrectionPreview?) {
        if (value == correction) return
        correction = value
        publish()
    }

    /** The gõ tắt trigger a `↩` chip would put back, or null when that beat is over. */
    fun showShortcut(value: String?) {
        if (value == shortcutUndo) return
        shortcutUndo = value
        publish()
    }

    /**
     * A tapped suggestion, re-indexed into the list its source gave: correction chips come
     * first and repeats are dropped, so a slot is not the source's index.
     */
    fun personalSelection(selection: SuggestionSelection): SuggestionSelection? {
        val index = personal.indexOf(selection.text)
        return if (index < 0) null else selection.copy(index = index)
    }

    private fun publish() {
        val next = SuggestionBarComposer.compose(correction, personal, shortcutUndo)
        if (next == shown) return
        shown = next
        runCatching { show(next) }
    }
}
