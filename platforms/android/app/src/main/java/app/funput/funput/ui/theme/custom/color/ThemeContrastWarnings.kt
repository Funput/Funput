package app.funput.funput.ui.theme.custom.color

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.validation.ThemeContrastPair
import app.funput.funput.theme.validation.ThemeValidator

/**
 * Flags color pairings that will be hard to read.
 *
 * Advisory only — it never blocks saving, and says so, because a theme is the user's to get wrong.
 * Someone deliberately building a very low-contrast look should be told once, not stopped.
 */
@Composable
internal fun ThemeContrastWarnings(theme: KeyboardTheme, modifier: Modifier = Modifier) {
    val issues = ThemeValidator.validate(theme)
    if (issues.isEmpty()) return

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            Text(
                text = stringResource(R.string.custom_theme_contrast_title),
                style = MaterialTheme.typography.titleSmall,
            )
            issues.forEach { issue ->
                Text(
                    text = stringResource(
                        R.string.custom_theme_contrast_item,
                        stringResource(issue.pair.labelRes),
                        issue.ratio,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = stringResource(R.string.custom_theme_contrast_footer),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
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
        ThemeContrastPair.SuggestionHighlightOnBackground ->
            R.string.custom_theme_contrast_suggestion
    }
