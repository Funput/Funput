package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardRow

internal object NumberKeyboardLayouts {
    fun resolve(inputMethod: KeyboardInputMethod, mode: KeyboardEditorMode): KeyboardLayout {
        require(mode.isNumber) { "Number layout requires a numeric editor mode" }
        return KeyboardLayout(
            id = "number-${mode.name.lowercase()}-${inputMethod.name.lowercase()}",
            inputMethod = inputMethod,
            suggestionBar = null,
            rows = listOf(
                row(digit('1'), digit('2'), digit('3'), command("backspace", KeyRole.BACKSPACE)),
                row(digit('4'), digit('5'), digit('6'), command("enter", KeyRole.ENTER)),
                row(digit('7'), digit('8'), digit('9'), text("period", ".", "Decimal point")),
                row(empty("left"), digit('0'), empty("right"), text("comma", ",", "Decimal comma")),
            ),
        )
    }

    private fun row(vararg keys: KeySpec) = KeyboardRow(keys.toList())

    private fun digit(value: Char) = KeySpec(
        id = "number-$value",
        label = value.toString(),
        role = KeyRole.CHARACTER,
        accessibilityLabel = value.toString(),
    )

    private fun command(id: String, role: KeyRole) = KeySpec(
        id = id,
        label = "",
        role = role,
        accessibilityLabel = id.replaceFirstChar(Char::uppercaseChar),
    )

    private fun text(id: String, value: String, accessibilityLabel: String) = KeySpec(
        id = id,
        label = value,
        role = KeyRole.PUNCTUATION,
        accessibilityLabel = accessibilityLabel,
    )

    private fun empty(position: String) = KeySpec(
        id = "placeholder-$position",
        label = "",
        role = KeyRole.PLACEHOLDER,
        accessibilityLabel = "Empty keypad position",
    )
}
