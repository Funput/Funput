package app.funput.funput.theme.store.custom

import app.funput.funput.theme.KeyboardTheme
import kotlin.math.roundToInt

/** Optional visual token overrides for a user-created theme. */
data class CustomThemeOverrides(
    val accentColor: Int? = null,
    val keyBackgroundOpacity: Float? = null,
) {
    init {
        require(keyBackgroundOpacity == null || keyBackgroundOpacity in 0f..1f) {
            "Key background opacity must be between 0 and 1"
        }
    }

    fun applyTo(baseTheme: KeyboardTheme): KeyboardTheme =
        baseTheme.copy(
            accentColor = accentColor ?: baseTheme.accentColor,
            keyColor = baseTheme.keyColor.withOpacity(keyBackgroundOpacity),
            specialKeyColor = baseTheme.specialKeyColor.withOpacity(keyBackgroundOpacity),
            pressedKeyColor = baseTheme.pressedKeyColor.withOpacity(keyBackgroundOpacity),
            activatedKeyColor = baseTheme.activatedKeyColor.withOpacity(keyBackgroundOpacity),
        )

    /**
     * Scales the existing alpha instead of replacing it, so the control only ever reduces what
     * the base theme already draws. A base whose key surfaces are fully transparent stays
     * plateless rather than gaining opaque black keys at the slider's default position.
     */
    private fun Int.withOpacity(opacity: Float?): Int {
        if (opacity == null) return this
        val alpha = ((this ushr AlphaShift) * opacity).roundToInt().coerceIn(0, MaxAlpha)
        return (this and RgbMask) or (alpha shl AlphaShift)
    }
}

private const val MaxAlpha = 255
private const val AlphaShift = 24
private const val RgbMask = 0x00FFFFFF
