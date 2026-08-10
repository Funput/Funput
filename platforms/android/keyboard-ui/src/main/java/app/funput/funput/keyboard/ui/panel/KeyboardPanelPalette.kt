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
