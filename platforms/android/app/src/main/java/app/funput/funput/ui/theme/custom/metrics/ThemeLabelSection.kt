package app.funput.funput.ui.theme.custom.metrics

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.theme.KeyboardLabelWeight
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.MetricClamp
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.rows.SegmentedRow

/**
 * Page "Phím & chữ": how big and how heavy the key labels are, so the text can be fitted to a
 * keyboard made taller or shorter. Only the drawn text changes; keys stay where they were.
 */
@Composable
internal fun ThemeLabelSection(
    theme: KeyboardTheme,
    onThemeChange: ((KeyboardTheme) -> KeyboardTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weights = KeyboardLabelWeight.entries
    FunputSection(title = stringResource(R.string.custom_theme_label_title), modifier = modifier) {
        ThemePercentSlider(
            label = stringResource(R.string.custom_theme_label_size),
            value = theme.labelScale,
            range = MetricClamp.LabelScale,
            onChange = { value -> onThemeChange { it.copy(labelScale = value) } },
        )
        FunputDivider()
        SegmentedRow(
            title = stringResource(R.string.custom_theme_label_weight),
            options = weights.map { stringResource(it.labelRes) },
            selectedIndex = weights.indexOf(theme.labelWeight),
            onSelect = { index -> onThemeChange { it.copy(labelWeight = weights[index]) } },
        )
    }
}

@get:StringRes
private val KeyboardLabelWeight.labelRes: Int
    get() = when (this) {
        KeyboardLabelWeight.LIGHT -> R.string.custom_theme_label_weight_light
        KeyboardLabelWeight.REGULAR -> R.string.custom_theme_label_weight_regular
        KeyboardLabelWeight.MEDIUM -> R.string.custom_theme_label_weight_medium
        KeyboardLabelWeight.BOLD -> R.string.custom_theme_label_weight_bold
    }
