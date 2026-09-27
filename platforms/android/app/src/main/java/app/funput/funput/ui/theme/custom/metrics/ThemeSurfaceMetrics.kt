package app.funput.funput.ui.theme.custom.metrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.MetricClamp
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection

/** Page "Phím & chữ": how the key surface itself is drawn, border to opacity. */
@Composable
internal fun ThemeSurfaceSection(
    theme: KeyboardTheme,
    onThemeChange: ((KeyboardTheme) -> KeyboardTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    FunputSection(title = stringResource(R.string.custom_theme_metrics_surface), modifier = modifier) {
        ThemeDpSlider(
            label = stringResource(R.string.custom_theme_metric_border_width),
            value = theme.keyBorderWidthDp,
            range = MetricClamp.BorderWidthDp,
            onChange = { value -> onThemeChange { it.copy(keyBorderWidthDp = value) } },
        )
        FunputDivider()
        ThemeDpSlider(
            label = stringResource(R.string.custom_theme_metric_shadow_offset),
            value = theme.keyShadowOffsetDp,
            range = MetricClamp.ShadowOffsetDp,
            onChange = { value -> onThemeChange { it.copy(keyShadowOffsetDp = value) } },
        )
        FunputDivider()
        ThemePercentSlider(
            label = stringResource(R.string.custom_theme_metric_key_opacity),
            value = theme.keyOpacity,
            range = MetricClamp.Opacity,
            onChange = { value -> onThemeChange { it.copy(keyOpacity = value) } },
        )
        FunputDivider()
        ThemePercentSlider(
            label = stringResource(R.string.custom_theme_metric_special_key_opacity),
            value = theme.specialKeyOpacity,
            range = MetricClamp.Opacity,
            onChange = { value -> onThemeChange { it.copy(specialKeyOpacity = value) } },
        )
    }
}

/** Page "Khi nhấn": how far a key grows under a finger. */
@Composable
internal fun ThemePressedSection(
    theme: KeyboardTheme,
    onThemeChange: ((KeyboardTheme) -> KeyboardTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    FunputSection(title = null, modifier = modifier) {
        ThemePercentSlider(
            label = stringResource(R.string.custom_theme_metric_pressed_scale),
            value = theme.pressedKeyScale,
            range = MetricClamp.PressedKeyScale,
            onChange = { value -> onThemeChange { it.copy(pressedKeyScale = value) } },
        )
    }
}
