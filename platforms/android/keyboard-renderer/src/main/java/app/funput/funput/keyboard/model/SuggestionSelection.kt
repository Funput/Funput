package app.funput.funput.keyboard.model

data class SuggestionSelection(
    val index: Int,
    val text: String,
) {
    init {
        require(index >= 0) { "Suggestion index must not be negative" }
        require(text.isNotBlank()) { "Suggestion text must not be blank" }
    }
}
