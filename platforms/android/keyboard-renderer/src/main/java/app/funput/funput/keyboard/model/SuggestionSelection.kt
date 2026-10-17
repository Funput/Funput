package app.funput.funput.keyboard.model

import app.funput.funput.keyboard.suggestion.SuggestionChipKind

data class SuggestionSelection(
    val index: Int,
    val text: String,
    val kind: SuggestionChipKind = SuggestionChipKind.SUGGESTION,
) {
    init {
        require(index >= 0) { "Suggestion index must not be negative" }
        require(text.isNotBlank()) { "Suggestion text must not be blank" }
    }
}
