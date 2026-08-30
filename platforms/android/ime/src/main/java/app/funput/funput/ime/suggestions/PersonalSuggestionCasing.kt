package app.funput.funput.ime.suggestions

import java.util.Locale

internal object PersonalSuggestionCasing {
    private val Vietnamese = Locale.forLanguageTag("vi-VN")

    /**
     * A prediction has no prefix to take its shape from, so [capitalized] — the
     * keyboard's own Shift state — is the only thing left to ask.
     */
    fun apply(candidate: String, prefix: String, capitalized: Boolean = false): String = when {
        prefix.isEmpty() -> if (capitalized) candidate.titlecased() else candidate
        prefix.hasLetters() && prefix == prefix.uppercase(Vietnamese) -> candidate.uppercase(Vietnamese)
        prefix.firstOrNull()?.isUpperCase() == true -> candidate.titlecased()
        else -> candidate.lowercase(Vietnamese)
    }

    private fun String.titlecased() = replaceFirstChar { char -> char.titlecase(Vietnamese) }

    private fun String.hasLetters() = any(Char::isLetter)
}
