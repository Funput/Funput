package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout
import app.funput.funput.keyboard.model.KeyboardRow

object KeyboardLayouts {
    fun forInputMethod(inputMethod: KeyboardInputMethod): KeyboardLayout = when (inputMethod) {
        KeyboardInputMethod.TELEX -> telex
        KeyboardInputMethod.VNI -> vni
    }

    val telex = create("qwerty-telex", KeyboardInputMethod.TELEX)
    val vni = create("qwerty-vni", KeyboardInputMethod.VNI, listOf(vniModifierRow()))

    private fun create(
        id: String,
        inputMethod: KeyboardInputMethod,
        leadingRows: List<KeyboardRow> = emptyList(),
    ) = qwertyLayout(id, inputMethod, leadingRows, standardActionKeys())

    private fun standardActionKeys() = listOf(
        specialKey("symbols", "?123", KeyRole.SYMBOLS, 1.25f, "Symbols"),
        specialKey("comma", ",", KeyRole.PUNCTUATION, 0.85f),
        standardSpaceKey(),
        specialKey("period", ".", KeyRole.PUNCTUATION, 0.85f),
        specialKey("enter", "", KeyRole.ENTER, 1.35f, "Enter"),
    )
}
