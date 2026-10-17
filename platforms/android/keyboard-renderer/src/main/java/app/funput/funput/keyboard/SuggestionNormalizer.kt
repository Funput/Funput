package app.funput.funput.keyboard

import app.funput.funput.keyboard.suggestion.SuggestionChip

internal object SuggestionNormalizer {
    private const val MaxVisibleSuggestions = 3

    fun normalize(suggestions: List<SuggestionChip>): List<SuggestionChip> = suggestions.asSequence()
        .map { it.copy(text = it.text.trim()) }
        .filter { it.text.isNotEmpty() }
        .take(MaxVisibleSuggestions)
        .toList()
}
