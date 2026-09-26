package app.funput.funput.ui.kit.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import app.funput.funput.ui.kit.tokens.ColorToken
import app.funput.funput.ui.kit.tokens.ColorTokens

/**
 * The colour roles of FunputUI, already resolved for one appearance.
 *
 * Components read roles, never hex values, so a token change reaches every screen at once and a
 * light/dark switch is one recomposition.
 */
@Immutable
class FunputColors internal constructor(
    /** Whether these are the dark-appearance values. */
    val isDark: Boolean,
    /** Funput orange: selection, links, primary buttons, focus. */
    val accent: Color,
    /** Text and icons drawn on an [accent] fill. */
    val onAccent: Color,
    /** The screen behind the cards. */
    val groupedBackground: Color,
    /** Cards and sheets. */
    val cardBackground: Color,
    /** The hairline around a card. */
    val cardStroke: Color,
    /** Primary text. */
    val label: Color,
    /** Summaries, values and secondary text. */
    val secondaryLabel: Color,
    /** Placeholders and disabled text. */
    val tertiaryLabel: Color,
    /** Dividers between rows. */
    val separator: Color,
    /** Confirmations and "ready" states. */
    val success: Color,
    /** Destructive actions and errors. */
    val destructive: Color,
) {
    internal companion object {
        /** Resolves every role for the light or dark appearance. */
        fun of(isDark: Boolean): FunputColors {
            fun ColorToken.pick() = if (isDark) dark else light
            return FunputColors(
                isDark = isDark,
                accent = ColorTokens.accent.pick(),
                onAccent = ColorTokens.onAccent.pick(),
                groupedBackground = ColorTokens.groupedBackground.pick(),
                cardBackground = ColorTokens.cardBackground.pick(),
                cardStroke = ColorTokens.cardStroke.pick(),
                label = ColorTokens.label.pick(),
                secondaryLabel = ColorTokens.secondaryLabel.pick(),
                tertiaryLabel = ColorTokens.tertiaryLabel.pick(),
                separator = ColorTokens.separator.pick(),
                success = ColorTokens.success.pick(),
                destructive = ColorTokens.destructive.pick(),
            )
        }
    }
}
