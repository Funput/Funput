package app.funput.funput.ui.theme.custom.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.MetricClamp

@Composable
internal fun ThemeMetricsTab(
    theme: KeyboardTheme,
    onThemeChange: ((KeyboardTheme) -> KeyboardTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = modifier) {
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
