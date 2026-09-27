package app.funput.funput.ui.theme.custom.color

import androidx.annotation.StringRes
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.validation.ThemeContrastPair
import app.funput.funput.theme.validation.ThemeValidator
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.rows.FunputRow
import app.funput.funput.ui.kit.theme.FunputUi
import java.util.Locale

/**
 * Flags colour pairings that will be hard to read, each with its measured ratio.
 *
 * Advisory only: it never blocks saving, and its footer says so, because a theme is the user's to
 * get wrong. Someone deliberately building a very low-contrast look should be told, not stopped.
 */
@Composable
internal fun ThemeContrastWarnings(theme: KeyboardTheme, modifier: Modifier = Modifier) {
    val issues = ThemeValidator.validate(theme)
    if (issues.isEmpty()) return
    val colors = FunputUi.colors
    FunputSection(
        title = stringResource(R.string.custom_theme_contrast_title),
        footer = stringResource(R.string.custom_theme_contrast_footer),
        modifier = modifier,
    ) {
        issues.forEachIndexed { index, issue ->
            if (index > 0) FunputDivider()
            FunputRow(
                title = stringResource(issue.pair.labelRes),
                detail = {
                    BasicText(
                        text = String.format(Locale.ROOT, "%.1f:1", issue.ratio),
                        style = FunputUi.typography.body.copy(color = colors.destructive),
                    )
                },
            )
        }
    }
}

@get:StringRes
private val ThemeContrastPair.labelRes: Int
    get() = when (this) {
        ThemeContrastPair.LabelOnKey -> R.string.custom_theme_contrast_label_on_key
        ThemeContrastPair.SpecialLabelOnSpecialKey -> R.string.custom_theme_contrast_special_label
        ThemeContrastPair.SecondaryLabelOnKey -> R.string.custom_theme_contrast_secondary_label
        ThemeContrastPair.AccentLabelOnAccentKey -> R.string.custom_theme_contrast_accent_label
        ThemeContrastPair.SuggestionHighlightOnBackground -> R.string.custom_theme_contrast_suggestion
    }
