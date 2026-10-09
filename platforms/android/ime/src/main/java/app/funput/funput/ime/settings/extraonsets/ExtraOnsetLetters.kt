package app.funput.funput.ime.settings.extraonsets

/** Immutable onset selection, encoded as a canonical `zfwj` subsequence without native bits. */
@JvmInline
value class ExtraOnsetLetters private constructor(
    /** Canonical string to write to Preferences DataStore. */
    val configurationValue: String,
) {
    /** Whether the master switch is on; removing the last letter turns it off. */
    val isEnabled: Boolean get() = configurationValue.isNotEmpty()

    /** Whether this selection permits [letter] as an onset. */
    operator fun contains(letter: ExtraOnsetLetter): Boolean = letter.spelling in configurationValue

    /** Adds or removes only [letter], preserving the canonical order. */
    fun withLetter(letter: ExtraOnsetLetter, enabled: Boolean): ExtraOnsetLetters {
        val updated = if (enabled) configurationValue + letter.spelling
        else configurationValue.filterNot { it == letter.spelling }
        return parse(updated)
    }

    /** Defaults and decoding for persisted selections. */
    companion object {
        /** Native Vietnamese spelling only; the default for old and new installations. */
        val None: ExtraOnsetLetters = ExtraOnsetLetters("")
        /** All four optional consonants, selected when the master switch is enabled. */
        val All: ExtraOnsetLetters = ExtraOnsetLetters(
            ExtraOnsetLetter.Ordered.joinToString("") { it.spelling.toString() }
        )

        /** Reads case-insensitively, ignores unknown letters and removes duplicates. */
        fun parse(value: String?): ExtraOnsetLetters = ExtraOnsetLetters(
            ExtraOnsetLetter.Ordered.filter { letter ->
                value?.any { it.equals(letter.spelling, ignoreCase = true) } == true
            }.joinToString("") { it.spelling.toString() }
        )
    }
}
