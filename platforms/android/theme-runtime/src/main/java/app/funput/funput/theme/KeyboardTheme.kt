package app.funput.funput.theme

/**
 * Renderer-agnostic visual tokens for a keyboard theme.
 *
 * Colors use Android ARGB ints, while dimensions are expressed in density-independent pixels.
 * Theme packages may provide these values later; keeping the contract here prevents themes from
 * changing keyboard geometry or input behavior.
 *
 * A fully transparent color means "draw nothing": the renderer skips the corresponding pass
 * instead of painting an invisible shape. This is what lets a theme drop key plates entirely.
 *
 * Adding a token: append it at the end with a default derived from an earlier token, and read it
 * as optional in the theme store codec. Existing presets, custom themes saved on disk, and
 * positional constructor calls all keep working, so a new theme never forces a renderer change.
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
    /** Label and icon color for modifier keys, so they can recede without needing a plate. */
    val specialLabelColor: Int = labelColor,
    /** Fill behind the Enter key, the one key a theme may promote to its accent color. */
    val accentKeyColor: Int = specialKeyColor,
    /** Label and icon color drawn on top of [accentKeyColor]. */
    val accentLabelColor: Int = accentColor,
    /** Surface of the magnified key preview, which must stay opaque even on plateless themes. */
    val popupSurfaceColor: Int = keyColor,
    /** Label color of the leading suggestion, the strongest candidate in the bar. */
    val suggestionHighlightColor: Int = labelColor,
    /** How much a key surface grows while held, as a multiplier of its resting size. */
    val pressedKeyScale: Float = 1f,
) {
    init {
        require(keyCornerRadiusDp >= 0f) { "Key corner radius must not be negative" }
        require(keyBorderWidthDp >= 0f) { "Key border width must not be negative" }
        require(keyShadowOffsetDp >= 0f) { "Key shadow offset must not be negative" }
        require(pressedKeyShadowOffsetDp >= 0f) { "Pressed key shadow offset must not be negative" }
        require(pressedKeyScale in 1f..MaxPressedKeyScale) {
            "Pressed key scale must be between 1 and $MaxPressedKeyScale"
        }
    }

    private companion object {
        const val MaxPressedKeyScale = 1.5f
    }
}
