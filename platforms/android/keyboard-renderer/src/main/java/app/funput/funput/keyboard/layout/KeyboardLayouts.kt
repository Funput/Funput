package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout

object KeyboardLayouts {
    fun forInputMethod(inputMethod: KeyboardInputMethod): KeyboardLayout = when (inputMethod) {
        KeyboardInputMethod.TELEX -> telex
        KeyboardInputMethod.VNI -> vni
    }

    val telex = create("qwerty-telex", KeyboardInputMethod.TELEX)
    val vni = create("qwerty-vni", KeyboardInputMethod.VNI)

    private fun create(id: String, inputMethod: KeyboardInputMethod) = qwertyLayout(
        id = id,
        inputMethod = inputMethod,
        leadingRows = listOf(topNumberRowForLetters(inputMethod)),
        actionKeys = standardActionKeys(),
        supportsVietnameseAlternates = true,
    )

    private fun standardActionKeys() = listOf(
        specialKey("symbols", "?123", KeyRole.SYMBOLS, 1.7f, "Symbols"),
        specialKey("comma", ",", KeyRole.PUNCTUATION),
        standardSpaceKey(),
        specialKey("period", ".", KeyRole.PUNCTUATION),
        specialKey("enter", "", KeyRole.ENTER, 1.7f, "Enter"),
    )
}
