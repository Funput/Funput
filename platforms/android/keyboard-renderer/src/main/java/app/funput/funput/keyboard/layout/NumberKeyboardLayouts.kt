package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout

internal object NumberKeyboardLayouts {
    fun resolve(
        inputMethod: KeyboardInputMethod,
        mode: KeyboardEditorMode,
        suggestionsEnabled: Boolean,
    ): KeyboardLayout {
        require(mode.isNumber) { "Number layout requires a numeric editor mode" }
        return KeyboardLayout(
            id = "number-${mode.name.lowercase()}-${inputMethod.name.lowercase()}",
            inputMethod = inputMethod,
            suggestionBar = keyboardToolbarSpec().copy(suggestionsEnabled = suggestionsEnabled),
            rows = listOf(
                keypadRow(keypadDigit('1'), keypadDigit('2'), keypadDigit('3'), backspace()),
                keypadRow(keypadDigit('4'), keypadDigit('5'), keypadDigit('6'), enter()),
                keypadRow(keypadDigit('7'), keypadDigit('8'), keypadDigit('9'), period(mode)),
                keypadRow(sign(mode), keypadDigit('0'), keypadEmpty("center"), comma(mode)),
            ),
        )
    }

    private fun backspace() = keypadCommand("backspace", KeyRole.BACKSPACE, "Backspace")
    private fun enter() = keypadCommand("enter", KeyRole.ENTER, "Enter")
    private fun sign(mode: KeyboardEditorMode) = if (mode.allowsSigned) {
        keypadText("minus", "-", "Minus")
    } else {
        keypadEmpty("sign")
    }

    private fun period(mode: KeyboardEditorMode) = if (mode.allowsDecimal) {
        keypadText("period", ".", "Decimal point")
    } else {
        keypadEmpty("period")
    }

    private fun comma(mode: KeyboardEditorMode) = if (mode.allowsDecimal) {
        keypadText("comma", ",", "Decimal comma")
    } else {
        keypadEmpty("comma")
    }
}
