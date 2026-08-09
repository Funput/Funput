package app.funput.funput.ui.theme.custom.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.funput.funput.R
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.MetricClamp

@Composable
internal fun ThemeMetricsTab(
    theme: KeyboardTheme,
    onThemeChange: ((KeyboardTheme) -> KeyboardTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fineTuning by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = modifier) {
        PresetRow(
            titleRes = R.string.custom_theme_shape_title,
            entries = KeyShapePreset.entries,
            selected = KeyShapePreset.nearest(theme),
            labelOf = { preset -> stringResource(preset.labelRes) },
            onSelected = { preset -> onThemeChange(preset::applyTo) },
        )
        PresetRow(
            titleRes = R.string.custom_theme_relief_title,
            entries = KeyReliefPreset.entries,
            selected = KeyReliefPreset.nearest(theme),
            labelOf = { preset -> stringResource(preset.labelRes) },
            onSelected = { preset -> onThemeChange(preset::applyTo) },
        )
        TextButton(onClick = { fineTuning = !fineTuning }) {
            Text(stringResource(R.string.custom_theme_metrics_fine_tune))
        }
        if (fineTuning) {
            MetricSection(R.string.custom_theme_metrics_shape) {
                ThemeDpSlider(
                    label = stringResource(R.string.custom_theme_metric_corner_radius),
                    value = theme.keyCornerRadiusDp,
                    range = MetricClamp.CornerRadiusDp,
                    onChange = { value -> onThemeChange { it.copy(keyCornerRadiusDp = value) } },
                )
                ThemeDpSlider(
                    label = stringResource(R.string.custom_theme_metric_keycap_inset),
                    value = theme.keycapInsetDp,
                    range = MetricClamp.KeycapInsetDp,
                    onChange = { value -> onThemeChange { it.copy(keycapInsetDp = value) } },
                    hint = stringResource(R.string.custom_theme_metric_keycap_inset_hint),
                )
                ThemePercentSlider(
                    label = stringResource(R.string.custom_theme_metric_pressed_scale),
                    value = theme.pressedKeyScale,
                    range = MetricClamp.PressedKeyScale,
                    onChange = { value -> onThemeChange { it.copy(pressedKeyScale = value) } },
                )
            }
            MetricSection(R.string.custom_theme_metrics_surface) {
                ThemeDpSlider(
                    label = stringResource(R.string.custom_theme_metric_border_width),
                    value = theme.keyBorderWidthDp,
                    range = MetricClamp.BorderWidthDp,
                    onChange = { value -> onThemeChange { it.copy(keyBorderWidthDp = value) } },
                )
                ThemeDpSlider(
                    label = stringResource(R.string.custom_theme_metric_shadow_offset),
                    value = theme.keyShadowOffsetDp,
                    range = MetricClamp.ShadowOffsetDp,
                    onChange = { value -> onThemeChange { it.copy(keyShadowOffsetDp = value) } },
                )
                ThemePercentSlider(
                    label = stringResource(R.string.custom_theme_metric_key_opacity),
                    value = theme.keyOpacity,
                    range = MetricClamp.Opacity,
                    onChange = { value -> onThemeChange { it.copy(keyOpacity = value) } },
                )
                ThemePercentSlider(
                    label = stringResource(R.string.custom_theme_metric_special_key_opacity),
                    value = theme.specialKeyOpacity,
                    range = MetricClamp.Opacity,
                    onChange = { value -> onThemeChange { it.copy(specialKeyOpacity = value) } },
                )
            }
            MetricSection(R.string.custom_theme_metrics_background) {
                GradientDirectionSelector(
                    selected = theme.backgroundGradientDirection,
                    onSelected = { direction ->
                        onThemeChange { it.copy(backgroundGradientDirection = direction) }
                    },
                )
            }
        }
    }
}

@Composable
private fun <T> PresetRow(
    titleRes: Int,
    entries: List<T>,
    selected: T,
    labelOf: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) = MetricSection(titleRes) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        entries.forEach { entry ->
            FilterChip(
                selected = entry == selected,
                onClick = { onSelected(entry) },
                label = { Text(labelOf(entry)) },
            )
        }
    }
}

@Composable
private fun MetricSection(titleRes: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
        )
        content()
    }
}
