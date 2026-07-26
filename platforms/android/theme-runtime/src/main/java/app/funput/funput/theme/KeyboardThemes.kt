package app.funput.funput.theme

/**
 * Named keyboard theme presets.
 *
 * Both presets are built from the same Funput accent gold, which is the only saturated color on
 * the keyboard and appears in exactly three places: the leading suggestion, the held key, and
 * Enter. Preset names describe the design; the persisted identifiers in [KeyboardThemeId] stay
 * `dark` and `light` because they are what the appearance setting stores on device.
 */
object KeyboardThemes {
    private const val AccentGold = 0xFFC8A951.toInt()

    /**
     * Dark preset. Borderless, with key plates carried by a faint white wash rather than a solid
     * color: translucency keeps the plates reading correctly over the background gradient, and
     * over a background image if one is set. Modifier keys wash in at half the strength of the
     * alphabet so the letters stay dominant while every key still shows its touch area.
     */
    val Ink: KeyboardTheme = KeyboardTheme(
        backgroundStartColor = 0xFF121013.toInt(),
        backgroundEndColor = 0xFF0A0A0C.toInt(),
        keyColor = 0x14FFFFFF,
        specialKeyColor = 0x0AFFFFFF,
        keyBorderColor = 0x00000000,
        keyShadowColor = 0x00000000,
        pressedKeyColor = 0x59C8A951,
        pressedKeyBorderColor = 0x00000000,
        activatedKeyColor = 0x3DC8A951,
        activatedKeyBorderColor = 0x00000000,
        labelColor = 0xFFF2F2F6.toInt(),
        secondaryLabelColor = 0xFF83838E.toInt(),
        accentColor = AccentGold,
        keyCornerRadiusDp = 12f,
        keyBorderWidthDp = 0f,
        keyShadowOffsetDp = 0f,
        pressedKeyShadowOffsetDp = 0f,
        specialLabelColor = 0xFF83838E.toInt(),
        accentKeyColor = AccentGold,
        accentLabelColor = 0xFF17110A.toInt(),
        popupSurfaceColor = 0xFF232026.toInt(),
        suggestionHighlightColor = 0xFFE3C371.toInt(),
        pressedKeyScale = 1.06f,
    )

    /** Light preset. Warm paper ground, white keycaps, and a single crisp contact shadow. */
    val Paper: KeyboardTheme = KeyboardTheme(
        backgroundStartColor = 0xFFF3EFE7.toInt(),
        backgroundEndColor = 0xFFE8E2D6.toInt(),
        keyColor = 0xFFFFFFFF.toInt(),
        specialKeyColor = 0xFFDED7C9.toInt(),
        keyBorderColor = 0x00000000,
        keyShadowColor = 0x29352C1E,
        pressedKeyColor = AccentGold,
        pressedKeyBorderColor = 0x00000000,
        activatedKeyColor = AccentGold,
        activatedKeyBorderColor = 0x00000000,
        labelColor = 0xFF1C1913.toInt(),
        secondaryLabelColor = 0xFF6B6459.toInt(),
        accentColor = 0xFFB08F32.toInt(),
        keyCornerRadiusDp = 10f,
        keyBorderWidthDp = 0f,
        keyShadowOffsetDp = 1.5f,
        pressedKeyShadowOffsetDp = 0f,
        specialLabelColor = 0xFF4C463A.toInt(),
        accentKeyColor = 0xFFB08F32.toInt(),
        // Dark rather than cream: cream on this gold measures 2.9:1, under the readability bar.
        accentLabelColor = 0xFF17110A.toInt(),
        popupSurfaceColor = 0xFFFFFFFF.toInt(),
        suggestionHighlightColor = 0xFF8A6D18.toInt(),
        pressedKeyScale = 1f,
    )
}
