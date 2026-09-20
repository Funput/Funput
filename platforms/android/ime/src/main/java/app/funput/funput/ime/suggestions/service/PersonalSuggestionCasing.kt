package app.funput.funput.ime.suggestions

import app.funput.funput.keyboard.model.ShiftState
import java.util.Locale

/**
 * The shape a suggestion is shown in.
 *
 * Learned words are stored lowercase and dictionary words keep their own case, so every
 * capital on the bar is one this object put there.
 *
 * The rule mirrors `classify_case` in the Rust shortcut expander, and iOS's
 * `SuggestionCaseStyle` mirrors it too — three copies that must agree.
 */
internal object PersonalSuggestionCasing {
    private val Vietnamese = Locale.forLanguageTag("vi-VN")

    /**
     * Shift outranks the prefix. Pressing it over an already typed `vi` is a request for
     * capitals, and accepting the suggestion rewrites those letters anyway — the accept
     * path deletes the prefix before it inserts.
     */
    fun apply(candidate: String, prefix: String, shift: ShiftState = ShiftState.OFF): String =
        when (shift) {
            ShiftState.CAPS_LOCK -> candidate.uppercase(Vietnamese)
            ShiftState.ON -> candidate.titlecased()
            ShiftState.OFF -> candidate.matching(prefix)
        }

    private fun String.matching(prefix: String): String {
        // Letters only, so `1v` is read by its `v` rather than by its digit.
        val letters = prefix.filter(Char::isLetter)
        val first = letters.firstOrNull() ?: return this
        if (!first.isUpperCase()) return this
        val rest = letters.drop(1)
        // Shouting takes two letters to say. A lone capital is how every sentence starts,
        // which is why the shortcut rule reads `V` as Title too.
        if (rest.isNotEmpty() && rest.all(Char::isUpperCase)) return uppercase(Vietnamese)
        // A prefix the user cased deliberately — `VNa`, `iOS` — is left to speak for
        // itself, the way `classify_case` returns nothing for it.
        return if (rest.none(Char::isUpperCase)) titlecased() else this
    }

    private fun String.titlecased() = replaceFirstChar { char -> char.titlecase(Vietnamese) }
}
