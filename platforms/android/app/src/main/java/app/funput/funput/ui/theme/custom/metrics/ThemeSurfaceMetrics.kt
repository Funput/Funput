package app.funput.funput.ui.theme.custom.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.MetricClamp

/** Page "Phím & chữ": how the key surface itself is drawn. */
@Composable
internal fun ThemeKeyMetrics(
    theme: KeyboardTheme,
    onThemeChange: ((KeyboardTheme) -> KeyboardTheme) -> Unit,
    modifier: Modifier = Modifier,
) = Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = modifier) {
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
}

/** Page "Khi nhấn": how far a key moves under a finger. */
@Composable
internal fun ThemePressedMetrics(
    theme: KeyboardTheme,
    onThemeChange: ((KeyboardTheme) -> KeyboardTheme) -> Unit,
    modifier: Modifier = Modifier,
) = MetricSection(R.string.custom_theme_metrics_shape) {
    ThemePercentSlider(
        label = stringResource(R.string.custom_theme_metric_pressed_scale),
        value = theme.pressedKeyScale,
        range = MetricClamp.PressedKeyScale,
        onChange = { value -> onThemeChange { it.copy(pressedKeyScale = value) } },
    )
}
