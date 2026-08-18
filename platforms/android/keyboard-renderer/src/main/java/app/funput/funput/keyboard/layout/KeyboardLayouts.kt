package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayout

object KeyboardLayouts {
    /**
     * Builds the letters layout for [inputMethod].
     *
     * Factory default keeps the number row so existing geometry tests stay stable; the IME
     * always passes the persisted preference (default off for Telex-family).
     */
    fun forInputMethod(
        inputMethod: KeyboardInputMethod,
        showsNumberRow: Boolean = true,
    ): KeyboardLayout = when (inputMethod) {
        KeyboardInputMethod.TELEX -> create("qwerty-telex", inputMethod, showsNumberRow)
        KeyboardInputMethod.TELEX_ADVANCED ->
            create("qwerty-telex-advanced", inputMethod, showsNumberRow)
        KeyboardInputMethod.VNI -> create("qwerty-vni", inputMethod, showsNumberRow = true)
    }

    val telex = forInputMethod(KeyboardInputMethod.TELEX, showsNumberRow = true)
    val telexAdvanced = forInputMethod(KeyboardInputMethod.TELEX_ADVANCED, showsNumberRow = true)
    val vni = forInputMethod(KeyboardInputMethod.VNI, showsNumberRow = true)

    private fun create(
        id: String,
        inputMethod: KeyboardInputMethod,
        showsNumberRow: Boolean,
    ): KeyboardLayout {
        val hasNumberRow = inputMethod == KeyboardInputMethod.VNI || showsNumberRow
        return qwertyLayout(
            id = if (hasNumberRow) id else "$id-compact",
            inputMethod = inputMethod,
            leadingRows = if (hasNumberRow) {
                listOf(topNumberRowForLetters(inputMethod))
            } else {
                emptyList()
            },
            actionKeys = standardActionKeys(),
            supportsVietnameseAlternates = true,
            showsTelexHints = inputMethod.isTelexFamily,
        )
    }

    private fun standardActionKeys() = listOf(
        specialKey("symbols", "?123", KeyRole.SYMBOLS, 1.7f, "Ký hiệu"),
        specialKey("comma", ",", KeyRole.PUNCTUATION, accessibilityLabel = "Dấu phẩy"),
        standardSpaceKey(),
        specialKey("period", ".", KeyRole.PUNCTUATION, accessibilityLabel = "Dấu chấm"),
        specialKey("enter", "", KeyRole.ENTER, 1.7f, "Enter"),
    )
}
