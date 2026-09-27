package app.funput.funput.ui.theme.custom.metrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.funput.funput.ui.kit.rows.SliderRow
import java.util.Locale
import kotlin.math.roundToInt

/** A theme measurement in dp, as a slider row with its value at the end. */
@Composable
internal fun ThemeDpSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    SliderRow(
        title = label,
        value = value,
        onValueChange = onChange,
        valueRange = range,
        valueLabel = String.format(Locale.ROOT, "%.0f dp", value),
        modifier = modifier,
    )
}

/** A theme proportion, as a slider row showing a percentage at the end. */
@Composable
internal fun ThemePercentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    SliderRow(
        title = label,
        value = value,
        onValueChange = onChange,
        valueRange = range,
        valueLabel = "${(value * PercentScale).roundToInt()}%",
        modifier = modifier,
    )
}

private const val PercentScale = 100f
