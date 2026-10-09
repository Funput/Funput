package app.funput.funput.keyboard.layout.editors

import app.funput.funput.keyboard.layout.keys.commaKey
import app.funput.funput.keyboard.layout.keys.keypadCommand
import app.funput.funput.keyboard.layout.keys.keypadDigit
import app.funput.funput.keyboard.layout.keys.keypadEmpty
import app.funput.funput.keyboard.layout.keys.keypadRow
import app.funput.funput.keyboard.layout.keys.periodKey
import app.funput.funput.keyboard.layout.letters.asciiSpaceKey
import app.funput.funput.keyboard.layout.letters.qwertyLayout
import app.funput.funput.keyboard.layout.letters.specialKey
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout

internal object PasswordKeyboardLayouts {
    fun text(inputMethod: KeyboardInputMethod): KeyboardLayout = qwertyLayout(
        id = "qwerty-password-${inputMethod.name.lowercase()}",
        inputMethod = inputMethod,
        leadingRows = listOf(keypadRow(*"1234567890".map(::keypadDigit).toTypedArray())),
        actionKeys = listOf(
            specialKey("symbols", "?123", KeyRole.SYMBOLS, 1.7f, "Ký hiệu"),
            commaKey("comma"),
            asciiSpaceKey(5.8f),
            periodKey("period"),
            specialKey("enter", "", KeyRole.ENTER, 1.7f, "Enter"),
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

    private fun backspace() = keypadCommand("backspace", KeyRole.BACKSPACE, "Xóa")
    private fun enter() = keypadCommand("enter", KeyRole.ENTER, "Enter")
}
