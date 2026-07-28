package app.funput.funput.theme

/**
 * Dark blue-grey preset inspired by the restrained, high-contrast surfaces of Gboard.
 *
 * Every value is a visual token: the preset cannot change rows, key weights, or touch targets.
 */
internal val SlateKeyboardTheme = KeyboardTheme(
    backgroundStartColor = 0xFF26373C.toInt(),
    backgroundEndColor = 0xFF26373C.toInt(),
    keyColor = 0xFF46565C.toInt(),
    specialKeyColor = 0xFF35474D.toInt(),
    keyBorderColor = 0x00000000,
    keyShadowColor = 0x29000000,
    pressedKeyColor = 0xFF5C6F76.toInt(),
    pressedKeyBorderColor = 0x00000000,
    activatedKeyColor = 0xFF4E7778.toInt(),
    activatedKeyBorderColor = 0x00000000,
    labelColor = 0xFFF1F5F6.toInt(),
    secondaryLabelColor = 0xFFB7C4C8.toInt(),
    accentColor = 0xFF80CBC4.toInt(),
    keyCornerRadiusDp = 6f,
    keyBorderWidthDp = 0f,
    keyShadowOffsetDp = 1f,
    pressedKeyShadowOffsetDp = 0f,
    specialLabelColor = 0xFFE2EAEC.toInt(),
    accentKeyColor = 0xFF80CBC4.toInt(),
    accentLabelColor = 0xFF173A3C.toInt(),
    popupSurfaceColor = 0xFF46565C.toInt(),
    suggestionHighlightColor = 0xFFA7E0DB.toInt(),
    pressedKeyScale = 1f,
    suggestionDividerColor = 0x335F747B,
    popupShadowColor = 0x66000000,
)
