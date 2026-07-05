package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout

internal object PasswordKeyboardLayouts {
    fun text(inputMethod: KeyboardInputMethod): KeyboardLayout = qwertyLayout(
        id = "qwerty-password-${inputMethod.name.lowercase()}",
        inputMethod = inputMethod,
        leadingRows = listOf(keypadRow(*"1234567890".map(::keypadDigit).toTypedArray())),
        actionKeys = listOf(
            specialKey("symbols", "?123", KeyRole.SYMBOLS, 1.25f, "Symbols"),
            specialKey("comma", ",", KeyRole.PUNCTUATION, 0.85f),
            asciiSpaceKey(5.3f),
            specialKey("period", ".", KeyRole.PUNCTUATION, 0.85f),
            specialKey("enter", "", KeyRole.ENTER, 1.35f, "Enter"),
        ),
        showSuggestionBar = false,
    )

    fun pin(inputMethod: KeyboardInputMethod) = KeyboardLayout(
        id = "pin-${inputMethod.name.lowercase()}",
        inputMethod = inputMethod,
        suggestionBar = null,
        rows = listOf(
            keypadRow(keypadDigit('1'), keypadDigit('2'), keypadDigit('3'), backspace()),
            keypadRow(keypadDigit('4'), keypadDigit('5'), keypadDigit('6'), enter()),
            keypadRow(keypadDigit('7'), keypadDigit('8'), keypadDigit('9'), keypadEmpty("pin-top")),
            keypadRow(
                keypadEmpty("pin-left"),
                keypadDigit('0'),
                keypadEmpty("pin-center"),
                keypadEmpty("pin-right"),
            ),
        ),
    )

    private fun backspace() = keypadCommand("backspace", KeyRole.BACKSPACE, "Backspace")
    private fun enter() = keypadCommand("enter", KeyRole.ENTER, "Enter")
}
