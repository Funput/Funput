package app.funput.funput.ui.theme.custom

import app.funput.funput.theme.KeyboardTheme

/**
 * Applies an accent color to every role that should follow it.
 *
 * Picking an accent used to change only [KeyboardTheme.accentColor], which left the Enter key and
 * the leading suggestion on the previous accent — choosing purple still gave you a gold Enter key.
 * The glyph drawn on top of the accent is derived from the accent's luminance so it stays legible
 * whichever color is chosen.
 */
internal fun KeyboardTheme.withAccent(color: Int): KeyboardTheme = copy(
    accentColor = color,
    accentKeyColor = color,
    accentLabelColor = if (color.isLight) DarkGlyph else LightGlyph,
    suggestionHighlightColor = color,
)

/**
 * Relative luminance against a white background, using the sRGB coefficients. Alpha is composited
 * first so a translucent accent is judged as it will actually be seen.
 */
private val Int.isLight: Boolean
    get() {
        val alpha = (this ushr 24) / MaxChannel
        val red = ((this shr 16) and ChannelMask) / MaxChannel
        val green = ((this shr 8) and ChannelMask) / MaxChannel
        val blue = (this and ChannelMask) / MaxChannel
        val luminance = RedWeight * red + GreenWeight * green + BlueWeight * blue
        return luminance * alpha + (1f - alpha) > LightThreshold
    }

private const val ChannelMask = 0xFF
private const val MaxChannel = 255f
private const val RedWeight = 0.2126f
private const val GreenWeight = 0.7152f
private const val BlueWeight = 0.0722f
private const val LightThreshold = 0.55f
private const val DarkGlyph = 0xFF17110A.toInt()
private const val LightGlyph = 0xFFFFF9EA.toInt()
