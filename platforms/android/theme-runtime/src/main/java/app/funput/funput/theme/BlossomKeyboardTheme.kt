package app.funput.funput.theme

/**
 * Light floral preset: a blush-to-lilac gradient under near-white keycaps.
 *
 * The keycaps keep a little translucency so the gradient tints them as it runs across the
 * keyboard, instead of five identical white rows stamped over it. Modifier keys wash in at half
 * that strength so the letters stay dominant while every key still shows its touch area.
 */
internal val BlossomKeyboardTheme = GradientThemePalette(
    backgroundStart = 0xFFFFE3EF.toInt(),
    backgroundEnd = 0xFFDCD6F7.toInt(),
    key = 0xF2FFFFFF.toInt(),
    specialKey = 0x8FFFFFFF.toInt(),
    label = 0xFF3A2233.toInt(),
    secondaryLabel = 0xFF7A5C72.toInt(),
    specialLabel = 0xFF5C3F55.toInt(),
    accent = 0xFFC24A80.toInt(),
    accentLabel = 0xFFFFF2F8.toInt(),
    popupSurface = 0xFFFFFBFD.toInt(),
    // Deeper than the accent rose: the leading suggestion has no plate under it, and the accent
    // alone measures 3.2:1 against the lilac end of the gradient.
    suggestionHighlight = 0xFFA83A6B.toInt(),
    divider = 0x338A6B80,
    keyShadow = 0x24512A47,
    keyShadowOffsetDp = 1.5f,
    popupShadow = 0x33512A47,
    // A translucent rose over pale keycaps barely registers, so press and shift stay near solid.
    pressedAlpha = 0xE0,
    activatedAlpha = 0xC2,
).toKeyboardTheme()
