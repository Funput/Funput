package app.funput.funput.theme

/**
 * Apple's Liquid Glass, rebuilt for the Canvas renderer.
 *
 * The iOS build gets this material from the system: `UIGlassEffect` samples the host behind the
 * keyboard and the keys are barely tinted, carried instead by the light caught along their edges.
 * Android has no equivalent — an IME window cannot read the app underneath it — so the renderer
 * supplies both halves itself: a defocused color field for the keys to stand over, and the lit
 * body and lensed edge on each key. The tokens are the dark half of the iOS `funputGlass` preset,
 * flattened for that missing blur: the same greys, the same 6dp corner, the same gold for emphasis.
 *
 * The gradient below is only the floor the field is built on, which is why it is a near-flat dark
 * grey. Give it a color of its own and the field stops reading as depth and starts reading as a
 * pattern the keyboard is wearing.
 *
 * Geometry matches every other preset — no keycap inset, 6dp corners, the 1.04 press of the
 * other two glass presets — so switching to this theme changes how a key looks and never how
 * large it is or where it can be pressed.
 */
internal val LiquidGlassKeyboardTheme = KeyboardTheme(
    backgroundStartColor = 0xFF25252B.toInt(),
    backgroundEndColor = 0xFF121216.toInt(),
    // Clear panes: the plate only has to lift the ground enough for the rim to have something
    // to sit on. Modifier keys use the darker iOS grey at the same alpha so they recede by hue.
    keyColor = 0x596C6C70,
    specialKeyColor = 0x59464649,
    keyBorderColor = 0x00000000,
    keyShadowColor = 0x00000000,
    pressedKeyColor = 0x66FFFFFF,
    // Not a border — with no border width authored, this is only read for the pressed halo.
    pressedKeyBorderColor = 0x99FFFFFF.toInt(),
    activatedKeyColor = 0x4DC8A951,
    activatedKeyBorderColor = 0x00000000,
    labelColor = 0xFFFFFFFF.toInt(),
    secondaryLabelColor = 0xD1EBEBF5.toInt(),
    accentColor = 0xFFC8A951.toInt(),
    keyCornerRadiusDp = 6f,
    keyBorderWidthDp = 0f,
    // Liquid Glass separates layers with a defined edge rather than a cast shadow.
    keyShadowOffsetDp = 0f,
    pressedKeyShadowOffsetDp = 0f,
    specialLabelColor = 0xF2EBEBF5.toInt(),
    accentKeyColor = 0xCCC8A951.toInt(),
    accentLabelColor = 0xFF17110A.toInt(),
    popupSurfaceColor = 0xF22C2C2E.toInt(),
    suggestionHighlightColor = 0xFFE3C371.toInt(),
    pressedKeyScale = 1.04f,
    backgroundGradientDirection = KeyboardThemeGradientDirection.VERTICAL,
    suggestionDividerColor = 0x24FFFFFF,
    popupShadowColor = 0x66000000,
    keySurfaceStyle = KeyboardKeySurfaceStyle.LIQUID_GLASS,
)
