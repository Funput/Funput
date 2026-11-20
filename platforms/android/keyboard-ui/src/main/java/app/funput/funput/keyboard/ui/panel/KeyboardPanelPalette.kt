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
    /** Translucent wash drawn over a pressed element; see [pressOverlay]. */
    val pressed: Int,
    /** Translucent surface of a card laid on the panel background; see [cardSurface]. */
    val cardSurface: Int,
) {
    fun solidSurface(surface: Int): Int = surface.compositedOver(backgroundEnd)

    fun readable(preferred: Int, minimum: Double = 4.5): Int =
        contrastForeground(preferred, intArrayOf(backgroundStart, backgroundEnd), minimum)

    fun readableOn(surface: Int, preferred: Int): Int =
        contrastForeground(preferred, intArrayOf(
            surface.compositedOver(backgroundStart), surface.compositedOver(backgroundEnd),
        ), 4.5)

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
            pressed = pressOverlay(theme.pressedKeyColor, theme.labelColor),
            cardSurface = cardSurface(theme),
        )

        /**
         * Cards take the key colour, left translucent so they follow the background gradient the
         * way keys do. A theme whose keys blend into its background gets a faint wash of readable
         * ink instead, or its cards would be invisible.
         */
        internal fun cardSurface(theme: KeyboardTheme): Int {
            val ends = intArrayOf(theme.backgroundStartColor, theme.backgroundEndColor)
            val stands = ends.all { contrast(theme.keyColor.compositedOver(it), it) >= MinCardContrast }
            if (stands) return theme.keyColor
            return (contrastForeground(theme.labelColor, ends, 4.5) and Rgb) or (FaintWashAlpha shl 24)
        }

        /**
         * A pressed key is repainted in [pressedKey], but a panel draws the press over its
         * content, so an opaque token is thinned until that content still reads through it.
         * A theme with no pressed colour falls back to a faint wash of its [label] ink.
         */
        internal fun pressOverlay(pressedKey: Int, label: Int): Int {
            val alpha = pressedKey ushr 24
            if (alpha == 0) return (label and Rgb) or (FaintWashAlpha shl 24)
            return (pressedKey and Rgb) or (minOf(alpha, MaxPressAlpha) shl 24)
        }

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
private const val Rgb = 0x00FFFFFF
private const val MaxPressAlpha = 0x66
private const val FaintWashAlpha = 0x1F

/** Least contrast between a card and the background behind it for the card to read as one. */
internal const val MinCardContrast = 1.12
private const val Black = -0x1000000
private const val White = -0x1
