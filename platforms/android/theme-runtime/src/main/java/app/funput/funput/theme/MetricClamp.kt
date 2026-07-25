package app.funput.funput.theme

/**
 * Safe ranges for every numeric theme token.
 *
 * [KeyboardTheme] rejects out-of-range values outright, which is right for themes written in code
 * — a bad preset is a bug and should fail loudly during development. Values arriving from outside
 * the app are different: a theme file edited by hand, restored from an old backup, or downloaded
 * later must never be able to crash the keyboard.
 *
 * Because the constructor throws, an unsafe [KeyboardTheme] can never exist to be repaired after
 * the fact. Callers that read external numbers must therefore coerce each value through these
 * ranges *before* constructing the theme — see the theme store's JSON decoder.
 */
object MetricClamp {
    val Opacity = 0f..1f
    val PressedKeyScale = 1f..1.5f
    val CornerRadiusDp = 0f..28f
    val BorderWidthDp = 0f..4f
    val ShadowOffsetDp = 0f..8f
    val KeycapInsetDp = 0f..8f
}
