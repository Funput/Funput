package app.funput.funput.theme

/**
 * The colors that separate one gradient preset from another, without the tokens they share.
 *
 * Presets in this family are built the same way: borderless plates over a two-stop background
 * gradient, and one accent color that also carries press and activation feedback. Deriving that
 * feedback from [accent] instead of writing it out per preset is what keeps a theme from ending up
 * with a rose Enter key and a gold press highlight.
 *
 * Defaults describe the dark end of the family. A light preset overrides the shadow and the
 * feedback strength, because a translucent accent painted over a pale ground barely registers.
 */
internal data class GradientThemePalette(
    val backgroundStart: Int,
    val backgroundEnd: Int,
    val key: Int,
    val specialKey: Int,
    val label: Int,
    val secondaryLabel: Int,
    val specialLabel: Int,
    val accent: Int,
    val accentLabel: Int,
    val popupSurface: Int,
    val suggestionHighlight: Int,
    val divider: Int,
    val gradientDirection: KeyboardThemeGradientDirection = KeyboardThemeGradientDirection.Default,
    val keyShadow: Int = Transparent,
    val keyShadowOffsetDp: Float = 0f,
    val popupShadow: Int = DefaultPopupShadow,
    val pressedAlpha: Int = DefaultPressedAlpha,
    val activatedAlpha: Int = DefaultActivatedAlpha,
    val pressedKeyScale: Float = DefaultPressedKeyScale,
) {
    init {
        require(pressedAlpha in AlphaRange) { "Pressed alpha must be within $AlphaRange" }
        require(activatedAlpha in AlphaRange) { "Activated alpha must be within $AlphaRange" }
    }

    /** Expands the palette into the full token set the renderer reads. */
    fun toKeyboardTheme(): KeyboardTheme = KeyboardTheme(
        backgroundStartColor = backgroundStart,
        backgroundEndColor = backgroundEnd,
        keyColor = key,
        specialKeyColor = specialKey,
        keyBorderColor = Transparent,
        keyShadowColor = keyShadow,
        pressedKeyColor = accent.withAlpha(pressedAlpha),
        pressedKeyBorderColor = Transparent,
        activatedKeyColor = accent.withAlpha(activatedAlpha),
        activatedKeyBorderColor = Transparent,
        labelColor = label,
        secondaryLabelColor = secondaryLabel,
        accentColor = accent,
        keyCornerRadiusDp = KeyCornerRadiusDp,
        keyBorderWidthDp = 0f,
        keyShadowOffsetDp = keyShadowOffsetDp,
        pressedKeyShadowOffsetDp = 0f,
        specialLabelColor = specialLabel,
        accentKeyColor = accent,
        accentLabelColor = accentLabel,
        popupSurfaceColor = popupSurface,
        suggestionHighlightColor = suggestionHighlight,
        pressedKeyScale = pressedKeyScale,
        backgroundGradientDirection = gradientDirection,
        suggestionDividerColor = divider,
        popupShadowColor = popupShadow,
    )

    private fun Int.withAlpha(alpha: Int): Int = (alpha shl AlphaShift) or (this and RgbMask)
}

private const val Transparent = 0x00000000
private const val DefaultPopupShadow = 0x66000000
private const val DefaultPressedAlpha = 0x66
private const val DefaultActivatedAlpha = 0x44
private const val DefaultPressedKeyScale = 1.04f

/** The borderless key shape every bundled preset shares with the iOS keyboard. */
private const val KeyCornerRadiusDp = 6f

private const val AlphaShift = 24
private const val RgbMask = 0x00FFFFFF
private val AlphaRange = 0x00..0xFF
