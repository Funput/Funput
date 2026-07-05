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
    val activatedKeyColor: Int,
    val activatedKeyBorderColor: Int,
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
}
