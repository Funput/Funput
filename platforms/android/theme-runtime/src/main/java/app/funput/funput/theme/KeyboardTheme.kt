package app.funput.funput.theme

/**
 * Renderer-agnostic visual tokens for a keyboard theme.
 *
 * Colors use Android ARGB ints, while dimensions are expressed in density-independent pixels.
 * Theme packages may provide these values later; keeping the contract here prevents themes from
 * changing keyboard geometry or input behavior.
 */
data class KeyboardTheme(
    val backgroundStartColor: Int,
    val backgroundEndColor: Int,
    val keyColor: Int,
    val specialKeyColor: Int,
    val keyBorderColor: Int,
    val keyShadowColor: Int,
    val pressedKeyColor: Int,
    val pressedKeyBorderColor: Int,
    val labelColor: Int,
    val secondaryLabelColor: Int,
    val accentColor: Int,
    val keyCornerRadiusDp: Float,
    val keyBorderWidthDp: Float,
    val keyShadowOffsetDp: Float,
    val pressedKeyShadowOffsetDp: Float,
) {
    init {
        require(keyCornerRadiusDp >= 0f) { "Key corner radius must not be negative" }
        require(keyBorderWidthDp >= 0f) { "Key border width must not be negative" }
        require(keyShadowOffsetDp >= 0f) { "Key shadow offset must not be negative" }
        require(pressedKeyShadowOffsetDp >= 0f) { "Pressed key shadow offset must not be negative" }
    }

    companion object {
        /** Built-in fallback that does not require blur or a runtime shader. */
        val Aurora: KeyboardTheme = KeyboardTheme(
            backgroundStartColor = 0xFF12182B.toInt(),
            backgroundEndColor = 0xFF090D18.toInt(),
            keyColor = 0x2EFFFFFF,
            specialKeyColor = 0x4AFFFFFF,
            keyBorderColor = 0x3DFFFFFF,
            keyShadowColor = 0x52000000,
            pressedKeyColor = 0x668DB8FF,
            pressedKeyBorderColor = 0xCCBBD2FF.toInt(),
            labelColor = 0xFFF7F9FF.toInt(),
            secondaryLabelColor = 0xB8E2E8F7.toInt(),
            accentColor = 0xFF8DB8FF.toInt(),
            keyCornerRadiusDp = 11f,
            keyBorderWidthDp = 0.75f,
            keyShadowOffsetDp = 2f,
            pressedKeyShadowOffsetDp = 0.5f,
        )
    }
}
