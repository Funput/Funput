package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout

internal object NumberKeyboardLayouts {
    fun resolve(inputMethod: KeyboardInputMethod, mode: KeyboardEditorMode): KeyboardLayout {
        require(mode.isNumber) { "Number layout requires a numeric editor mode" }
        return KeyboardLayout(
            id = "number-${mode.name.lowercase()}-${inputMethod.name.lowercase()}",
            inputMethod = inputMethod,
            suggestionBar = null,
            rows = listOf(
                keypadRow(keypadDigit('1'), keypadDigit('2'), keypadDigit('3'), backspace()),
                keypadRow(keypadDigit('4'), keypadDigit('5'), keypadDigit('6'), enter()),
                keypadRow(keypadDigit('7'), keypadDigit('8'), keypadDigit('9'), period()),
                keypadRow(keypadEmpty("left"), keypadDigit('0'), keypadEmpty("right"), comma()),
            ),
        )
    }

    private fun backspace() = keypadCommand("backspace", KeyRole.BACKSPACE, "Backspace")
    private fun enter() = keypadCommand("enter", KeyRole.ENTER, "Enter")
    private fun period() = keypadText("period", ".", "Decimal point")
    private fun comma() = keypadText("comma", ",", "Decimal comma")
}
