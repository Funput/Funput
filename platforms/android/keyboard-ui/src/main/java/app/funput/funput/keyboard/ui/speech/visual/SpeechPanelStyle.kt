package app.funput.funput.keyboard.ui.speech.visual

import app.funput.funput.keyboard.ui.panel.KeyboardPanelPalette
import app.funput.funput.theme.KeyboardTheme

/** Derived entirely from the current keyboard theme, including saved custom themes. */
internal data class SpeechPanelStyle(
    val primarySurface: Int,
    val primaryLabel: Int,
    val secondarySurface: Int,
    val secondaryLabel: Int,
    val orbTint: Int,
) {
    companion object {
        fun from(theme: KeyboardTheme): SpeechPanelStyle {
            val palette = KeyboardPanelPalette.from(theme)
            val primary = palette.solidSurface(theme.accentKeyColor)
            val secondary = palette.solidSurface(theme.specialKeyColor)
            return SpeechPanelStyle(
                primarySurface = primary,
                primaryLabel = palette.readableOn(primary, theme.accentLabelColor),
                secondarySurface = secondary,
                secondaryLabel = palette.readableOn(secondary, theme.specialLabelColor),
                orbTint = theme.accentColor or 0xFF000000.toInt(),
            )
        }
    }
}
