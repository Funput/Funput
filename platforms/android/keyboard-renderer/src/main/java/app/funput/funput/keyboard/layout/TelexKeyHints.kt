package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyboardInputMethod

/** Corner hints for Telex modifier keys on the QWERTY letter rows. */
internal object TelexKeyHints {
    private val hints = mapOf(
        's' to "´",
        'f' to "`",
        'r' to "̉",
        'x' to "˜",
        'j' to "̣",
        'z' to "×",
        'd' to "đ",
        'w' to "˘+",
        'a' to "ˆ",
        'e' to "ˆ",
        'o' to "ˆ",
    )

    fun secondaryLabel(inputMethod: KeyboardInputMethod, character: Char): String? =
        if (inputMethod == KeyboardInputMethod.TELEX) hints[character] else null
}
