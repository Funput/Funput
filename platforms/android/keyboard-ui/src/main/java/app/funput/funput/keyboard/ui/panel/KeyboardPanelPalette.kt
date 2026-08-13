package app.funput.funput.keyboard.ui.panel

import app.funput.funput.theme.KeyboardTheme

internal data class KeyboardPanelPalette(
    val backgroundStart: Int,
    val backgroundEnd: Int,
    val label: Int,
    val secondaryLabel: Int,
    val accent: Int,
    val divider: Int,
    val searchSurface: Int,
    val buttonSurface: Int,
) {
    fun readable(preferred: Int, minimum: Double = 4.5): Int =
        contrastForeground(preferred, intArrayOf(backgroundStart, backgroundEnd), minimum)

    fun readableOn(surface: Int, preferred: Int): Int =
        contrastForeground(preferred, intArrayOf(surface), 4.5)

    companion object {
        fun from(theme: KeyboardTheme) = KeyboardPanelPalette(
            backgroundStart = theme.backgroundStartColor,
            backgroundEnd = theme.backgroundEndColor,
            label = theme.labelColor,
            secondaryLabel = theme.secondaryLabelColor,
            accent = theme.accentColor,
            divider = theme.suggestionDividerColor,
            searchSurface = theme.keyColor.compositedOver(theme.backgroundEndColor),
            buttonSurface = theme.specialKeyColor,
        )

        private fun Int.compositedOver(background: Int): Int {
            val alpha = (this ushr 24) / 255f
            fun channel(shift: Int): Int {
                val foreground = (this shr shift) and 0xFF
                val behind = (background shr shift) and 0xFF
                return (foreground * alpha + behind * (1f - alpha)).toInt()
            }
            return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
        }
    }
}

internal fun contrastForeground(preferred: Int, surfaces: IntArray, minimum: Double): Int {
    val candidates = intArrayOf(preferred or Opaque, Black, White)
    return candidates.firstOrNull { color -> surfaces.all { contrast(color, it) >= minimum } }
        ?: candidates.maxBy { color -> surfaces.minOf { contrast(color, it) } }
}

private fun contrast(first: Int, second: Int): Double {
    val lighter = maxOf(luminance(first), luminance(second))
    val darker = minOf(luminance(first), luminance(second))
    return (lighter + 0.05) / (darker + 0.05)
}

private fun luminance(color: Int): Double {
    fun channel(shift: Int): Double {
        val value = ((color shr shift) and 0xff) / 255.0
        return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
    }
    return channel(16) * 0.2126 + channel(8) * 0.7152 + channel(0) * 0.0722
}

private const val Opaque = -0x1000000
private const val Black = -0x1000000
private const val White = -0x1
