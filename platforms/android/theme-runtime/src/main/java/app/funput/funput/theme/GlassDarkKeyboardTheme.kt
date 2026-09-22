package app.funput.funput.theme

/**
 * Dark glass preset. Translucent key plates sit over a diagonal midnight gradient, while
 * borderless plates keep the surface quiet. Funput gold remains the only saturated color and
 * is reserved for emphasis and interaction.
 */
internal val GlassDarkKeyboardTheme = KeyboardTheme(
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
    accentColor = 0xFFC8A951.toInt(),
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
