package app.funput.funput.theme

/**
 * Dark floral preset: an orchid-to-midnight gradient under translucent plates.
 *
 * Plates are a faint white wash rather than a solid fill, which keeps the violet visible through
 * the whole keyboard and lets the same wash sit over a background image if one is set. Orchid pink
 * is the only saturated color and marks the leading suggestion, the held key, and Enter.
 *
 * The gradient runs the opposite way to [BlossomKeyboardTheme], so the pair reads as two sides of
 * one design rather than the same picture twice.
 */
internal val OrchidKeyboardTheme = GradientThemePalette(
    backgroundStart = 0xFF421650.toInt(),
    backgroundEnd = 0xFF140A22.toInt(),
    key = 0x1FFFFFFF,
    specialKey = 0x12FFFFFF,
    label = 0xFFF9EFF7.toInt(),
    secondaryLabel = 0xFFBBA0C8.toInt(),
    specialLabel = 0xFFC9B4D6.toInt(),
    accent = 0xFFE86FA9.toInt(),
    accentLabel = 0xFF2A0E22.toInt(),
    popupSurface = 0xFF2A163A.toInt(),
    suggestionHighlight = 0xFFFFA8CE.toInt(),
    divider = 0x2BFFFFFF,
    gradientDirection = KeyboardThemeGradientDirection.DIAGONAL_UP,
    pressedKeyScale = 1.06f,
).toKeyboardTheme()
