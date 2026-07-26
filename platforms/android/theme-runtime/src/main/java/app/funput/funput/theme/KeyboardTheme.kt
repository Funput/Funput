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
    /**
     * Scales the alpha of character key surfaces at draw time.
     *
     * Kept apart from the key colors so a theme can dim every key at once without the authored
     * colors being rewritten, and so repeatedly moving the control cannot compound.
     */
    val keyOpacity: Float = 1f,
    /** The same, for modifier keys. */
    val specialKeyOpacity: Float = 1f,
    /**
     * Shrinks the drawn key inside its touch target.
     *
     * The hit area never changes, which is what keeps a theme from altering where a key can be
     * pressed; only the painted surface moves inward.
     */
    val keycapInsetDp: Float = 0f,
    val backgroundGradientDirection: KeyboardThemeGradientDirection =
        KeyboardThemeGradientDirection.Default,
    /**
     * Separator between suggestions.
     *
     * The renderer used to force this to the secondary label color at a fixed low alpha. The
     * default reproduces exactly that, so themes saved before the token existed keep the divider
     * they were designed with instead of gaining an opaque line.
     */
    val suggestionDividerColor: Int =
        (secondaryLabelColor and RgbMask) or LegacyDividerAlpha,
    /** Shadow under the magnified key preview, previously a hardcoded black. */
    val popupShadowColor: Int = DefaultPopupShadowColor,
) {
    init {
        require(keyCornerRadiusDp >= 0f) { "Key corner radius must not be negative" }
        require(keyBorderWidthDp >= 0f) { "Key border width must not be negative" }
        require(keyShadowOffsetDp >= 0f) { "Key shadow offset must not be negative" }
        require(pressedKeyShadowOffsetDp >= 0f) { "Pressed key shadow offset must not be negative" }
        require(pressedKeyScale in MetricClamp.PressedKeyScale) {
            "Pressed key scale must be within ${MetricClamp.PressedKeyScale}"
        }
        require(keyOpacity in MetricClamp.Opacity) { "Key opacity must be within 0..1" }
        require(specialKeyOpacity in MetricClamp.Opacity) { "Special key opacity must be within 0..1" }
        require(keycapInsetDp >= 0f) { "Keycap inset must not be negative" }
    }

    private companion object {
        const val DefaultPopupShadowColor = 0x40000000
        const val RgbMask = 0x00FFFFFF
        const val LegacyDividerAlpha = 0x33000000
    }
}
