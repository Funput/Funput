package app.funput.funput.keyboard

internal object SuggestionNormalizer {
    private const val MaxVisibleSuggestions = 3

    fun normalize(suggestions: List<String>): List<String> = suggestions.asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .take(MaxVisibleSuggestions)
        .toList()
}
