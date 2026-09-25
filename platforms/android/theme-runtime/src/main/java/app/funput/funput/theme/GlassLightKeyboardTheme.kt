package app.funput.funput.theme

/**
 * Light glass preset based on Funput Glass for iOS. Cool translucent plates sit on a soft
 * blue-grey ground, while the borderless 6dp shape matches the approved Android dark preset.
 */
internal val GlassLightKeyboardTheme = KeyboardTheme(
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
