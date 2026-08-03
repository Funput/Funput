package app.funput.funput.ime.editing.composition

import app.funput.funput.keyboard.model.KeyboardInputMethod

/** Mirrors the shared engine's current word-boundary contract. */
internal object CompositionBoundary {
    fun isBoundary(codePoint: Int): Boolean =
        Character.isWhitespace(codePoint) || codePoint.isAsciiPunctuation()

    fun isBoundary(codePoint: Int, inputMethod: KeyboardInputMethod): Boolean =
        isBoundary(codePoint) && !codePoint.isAdvancedTelexShortcut(inputMethod)

    private fun Int.isAdvancedTelexShortcut(inputMethod: KeyboardInputMethod): Boolean =
        inputMethod == KeyboardInputMethod.TELEX_ADVANCED && (this == '['.code || this == ']'.code)

    private fun Int.isAsciiPunctuation(): Boolean =
        this in 0x21..0x2F ||
            this in 0x3A..0x40 ||
            this in 0x5B..0x60 ||
            this in 0x7B..0x7E
}
