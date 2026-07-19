package app.funput.funput.ime.suggestions

import java.util.Locale

internal object PersonalSuggestionCasing {
    private val Vietnamese = Locale.forLanguageTag("vi-VN")

    fun apply(candidate: String, prefix: String): String = when {
        prefix.hasLetters() && prefix == prefix.uppercase(Vietnamese) -> candidate.uppercase(Vietnamese)
        prefix.firstOrNull()?.isUpperCase() == true -> candidate.replaceFirstChar { char ->
            char.titlecase(Vietnamese)
        }
        else -> candidate.lowercase(Vietnamese)
    }

    private fun String.hasLetters() = any(Char::isLetter)
}
