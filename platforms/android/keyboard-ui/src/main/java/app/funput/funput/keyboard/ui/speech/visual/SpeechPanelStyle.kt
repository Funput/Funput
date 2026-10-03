package app.funput.funput.keyboard.ui.speech.visual

import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import app.funput.funput.theme.KeyboardTheme

/** Derived entirely from the current keyboard theme, including saved custom themes. */
internal data class SpeechPanelStyle(
    val radiusDp: Float,
    val primarySurface: Int,
    val primaryLabel: Int,
    val secondarySurface: Int,
    val secondaryLabel: Int,
    val cardSurface: Int,
    val cardLabel: Int,
    val cardSecondary: Int,
    val indicator: Int,
) {
    companion object {
        fun from(theme: KeyboardTheme): SpeechPanelStyle {
            val palette = KeyboardPanelPalette.from(theme)
            val primary = palette.solidSurface(theme.accentKeyColor)
            val secondary = palette.solidSurface(theme.specialKeyColor)
            return SpeechPanelStyle(
                radiusDp = (theme.keyCornerRadiusDp * 2).coerceIn(12f, 24f),
                primarySurface = primary,
                primaryLabel = palette.readableOn(primary, theme.accentLabelColor),
                secondarySurface = secondary,
                secondaryLabel = palette.readableOn(secondary, theme.specialLabelColor),
                cardSurface = palette.searchSurface,
                cardLabel = palette.readableOn(palette.searchSurface, theme.labelColor),
                cardSecondary = palette.readableOn(palette.searchSurface, theme.secondaryLabelColor),
                indicator = palette.readable(theme.accentColor),
            )
        }
    }
}
