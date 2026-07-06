package app.funput.funput.ime.editing

import app.funput.funput.keyboard.model.SuggestionSelection

/** Owns app-provided completions without retaining them after the editor changes. */
internal class EditorCompletionSession<T>(
    private val text: (T) -> CharSequence?,
    private val commit: (T) -> Boolean,
    private val onSuggestionsChanged: (List<String>) -> Unit,
) {
    private var enabled = false
    private var completions: List<T> = emptyList()

    fun configure(enabled: Boolean) {
        this.enabled = enabled
        clear()
    }

    fun update(values: Array<out T>?) {
        if (!enabled) return clear()
        completions = (values?.asSequence() ?: emptySequence())
            .filter { completion -> !text(completion).isNullOrEmpty() }
            .take(MaxSuggestions)
            .toList()
        onSuggestionsChanged(completions.map { completion -> text(completion).toString() })
    }

    fun select(selection: SuggestionSelection): Boolean {
        if (!enabled) return false
        return completions.getOrNull(selection.index)?.let(commit) == true
    }

    private fun clear() {
        completions = emptyList()
        onSuggestionsChanged(emptyList())
    }

    private companion object {
        const val MaxSuggestions = 3
    }
}
