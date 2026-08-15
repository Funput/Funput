package app.funput.funput.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.funput.funput.R
import app.funput.funput.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * A continuous setting, read as a percentage.
 *
 * The value is kept locally while the thumb moves and reported once on release: a store write per
 * frame of a drag would be dozens of writes for one decision. The readout still tracks the thumb,
 * so the number moves with the finger even though nothing is saved yet.
 */
@Composable
internal fun SettingsPercentSliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueSettled: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(value) { mutableFloatStateOf(value) }
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            // The section header carries its own bottom padding; matching it here keeps the
            // readout on the header's baseline rather than floating above it.
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SettingsSectionHeader(title, modifier = Modifier.weight(1f))
            Text(
                text = percentLabel(draft),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.Small),
            )
        }
        Slider(
            value = draft,
            valueRange = range,
            onValueChange = { draft = it },
            onValueChangeFinished = { onValueSettled(roundToPercent(draft)) },
            modifier = Modifier.semantics { contentDescription = title },
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Small),
        ) {
            SliderBound(percentLabel(range.start))
            SliderBound(percentLabel(range.endInclusive))
        }
    }
}

@Composable
private fun SliderBound(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun percentLabel(value: Float): String =
    stringResource(R.string.settings_percent_value, (value * PercentScale).roundToInt())

/** Whole percents only — a keyboard 3 thousandths taller is not a size anybody asked for. */
private fun roundToPercent(value: Float): Float =
    (value * PercentScale).roundToInt() / PercentScale

private const val PercentScale = 100f
