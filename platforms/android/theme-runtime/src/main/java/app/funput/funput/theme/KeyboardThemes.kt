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

    /**
     * Dark glass preset. Translucent key plates sit over a diagonal midnight gradient, while a
     * borderless plates keep the surface quiet. Funput gold remains the only saturated color and
     * is reserved for emphasis and interaction.
     */
    val GlassDark: KeyboardTheme = KeyboardTheme(
        backgroundStartColor = 0xFF151822.toInt(),
        backgroundEndColor = 0xFF08090D.toInt(),
        keyColor = 0x20FFFFFF,
        specialKeyColor = 0x12FFFFFF,
        keyBorderColor = 0x00000000,
        keyShadowColor = 0x66000000,
        pressedKeyColor = 0x66C8A951,
        pressedKeyBorderColor = 0xB3E3C371.toInt(),
        activatedKeyColor = 0x44C8A951,
        activatedKeyBorderColor = 0x00000000,
        labelColor = 0xFFF4F5FA.toInt(),
        secondaryLabelColor = 0xFF9094A3.toInt(),
        accentColor = AccentGold,
        keyCornerRadiusDp = 6f,
        keyBorderWidthDp = 0f,
        keyShadowOffsetDp = 1f,
        pressedKeyShadowOffsetDp = 0f,
        specialLabelColor = 0xFFB3B6C2.toInt(),
        accentKeyColor = 0xD0C8A951.toInt(),
        accentLabelColor = 0xFF17110A.toInt(),
        popupSurfaceColor = 0xFF1C1F29.toInt(),
        suggestionHighlightColor = 0xFFE3C371.toInt(),
        pressedKeyScale = 1.04f,
        suggestionDividerColor = 0x24FFFFFF,
        popupShadowColor = 0x66000000,
        keySurfaceStyle = KeyboardKeySurfaceStyle.GLASS,
    )

    /**
     * Light glass preset based on Funput Glass for iOS. Cool translucent plates sit on a soft
     * blue-grey ground, while the borderless 6dp shape matches the approved Android dark preset.
     */
    val GlassLight: KeyboardTheme = KeyboardTheme(
        backgroundStartColor = 0xFFDCE4ED.toInt(),
        backgroundEndColor = 0xFFAEBCCC.toInt(),
        keyColor = 0xB8FFFFFF.toInt(),
        specialKeyColor = 0xD1AAB7C7.toInt(),
        keyBorderColor = 0x00000000,
        keyShadowColor = 0x29000000,
        pressedKeyColor = 0x66A98B32,
        pressedKeyBorderColor = 0xB3A98B32.toInt(),
        activatedKeyColor = 0x44A98B32,
        activatedKeyBorderColor = 0x00000000,
        labelColor = 0xFF111318.toInt(),
        secondaryLabelColor = 0xFF414957.toInt(),
        accentColor = 0xFFA98B32.toInt(),
        keyCornerRadiusDp = 6f,
        keyBorderWidthDp = 0f,
        keyShadowOffsetDp = 1f,
        pressedKeyShadowOffsetDp = 0f,
        specialLabelColor = 0xFF303844.toInt(),
        accentKeyColor = 0xD0A98B32.toInt(),
        accentLabelColor = 0xFF17110A.toInt(),
        popupSurfaceColor = 0xFFF7F9FC.toInt(),
        suggestionHighlightColor = 0xFF755D16.toInt(),
        pressedKeyScale = 1.04f,
        suggestionDividerColor = 0x24414957,
        popupShadowColor = 0x52000000,
        keySurfaceStyle = KeyboardKeySurfaceStyle.GLASS,
    )
}
