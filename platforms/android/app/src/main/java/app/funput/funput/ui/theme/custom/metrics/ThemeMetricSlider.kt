package app.funput.funput.ui.theme.custom.metrics

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.funput.funput.R
import kotlin.math.roundToInt

/** A labelled slider showing its value in dp. */
@Composable
internal fun ThemeDpSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.custom_theme_metric_dp, label, value),
            style = MaterialTheme.typography.labelLarge,
        )
        hint?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Slider(
            value = value,
            valueRange = range,
            onValueChange = onChange,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

/** A labelled slider showing its value as a percentage of the given range. */
@Composable
internal fun ThemePercentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(
                R.string.custom_theme_metric_percent,
                label,
                (value * PercentScale).roundToInt(),
            ),
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = value,
            valueRange = range,
            onValueChange = onChange,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

private const val PercentScale = 100f
