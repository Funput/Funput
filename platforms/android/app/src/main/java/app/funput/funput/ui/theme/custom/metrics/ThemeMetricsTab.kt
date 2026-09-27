package app.funput.funput.ui.theme.custom.metrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.MetricClamp
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.rows.SegmentedRow
import app.funput.funput.ui.kit.rows.ToggleRow

/**
 * The shape of the keys, as answers rather than numbers: how round, and how raised. The exact
 * radius and inset stay one switch away for anyone who wants 9 instead of 8.
 */
@Composable
internal fun ThemeShapeSection(
    theme: KeyboardTheme,
    onThemeChange: ((KeyboardTheme) -> KeyboardTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fineTuning by rememberSaveable { mutableStateOf(false) }
    val shapes = KeyShapePreset.entries
    val reliefs = KeyReliefPreset.entries
    FunputSection(
        title = stringResource(R.string.custom_theme_metrics_shape),
        footer = if (fineTuning) stringResource(R.string.custom_theme_metric_keycap_inset_hint) else null,
        modifier = modifier,
    ) {
        SegmentedRow(
            title = stringResource(R.string.custom_theme_shape_title),
            options = shapes.map { stringResource(it.labelRes) },
            selectedIndex = shapes.indexOf(KeyShapePreset.nearest(theme)),
            onSelect = { index -> onThemeChange(shapes[index]::applyTo) },
        )
        FunputDivider()
        SegmentedRow(
            title = stringResource(R.string.custom_theme_relief_title),
            options = reliefs.map { stringResource(it.labelRes) },
            selectedIndex = reliefs.indexOf(KeyReliefPreset.nearest(theme)),
            onSelect = { index -> onThemeChange(reliefs[index]::applyTo) },
        )
        FunputDivider()
        ToggleRow(
            title = stringResource(R.string.custom_theme_metrics_fine_tune),
            checked = fineTuning,
            onCheckedChange = { fineTuning = it },
        )
        if (fineTuning) {
            FunputDivider()
            ThemeDpSlider(
                label = stringResource(R.string.custom_theme_metric_corner_radius),
                value = theme.keyCornerRadiusDp,
                range = MetricClamp.CornerRadiusDp,
                onChange = { value -> onThemeChange { it.copy(keyCornerRadiusDp = value) } },
            )
            FunputDivider()
            ThemeDpSlider(
                label = stringResource(R.string.custom_theme_metric_keycap_inset),
                value = theme.keycapInsetDp,
                range = MetricClamp.KeycapInsetDp,
                onChange = { value -> onThemeChange { it.copy(keycapInsetDp = value) } },
            )
        }
    }
}
