package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout

internal object PhoneKeyboardLayouts {
    fun resolve(inputMethod: KeyboardInputMethod, suggestionsEnabled: Boolean) = KeyboardLayout(
        id = "phone-${inputMethod.name.lowercase()}",
        inputMethod = inputMethod,
        suggestionBar = keyboardToolbarSpec().copy(suggestionsEnabled = suggestionsEnabled),
        rows = listOf(
            keypadRow(keypadDigit('1'), keypadDigit('2'), keypadDigit('3'), backspace()),
            keypadRow(keypadDigit('4'), keypadDigit('5'), keypadDigit('6'), enter()),
            keypadRow(
                keypadDigit('7'),
                keypadDigit('8'),
                keypadDigit('9'),
                phoneKey("plus", "+", "Plus"),
            ),
            keypadRow(
                phoneKey("star", "*", "Star"),
                keypadDigit('0'),
                phoneKey("hash", "#", "Hash"),
                keypadEmpty("phone"),
            ),
        ),
    )

    private fun backspace() = keypadCommand("backspace", KeyRole.BACKSPACE, "Backspace")
    private fun enter() = keypadCommand("enter", KeyRole.ENTER, "Enter")
    private fun phoneKey(id: String, value: String, label: String) = keypadText(id, value, label)
}
