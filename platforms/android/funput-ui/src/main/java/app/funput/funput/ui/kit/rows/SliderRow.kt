package app.funput.funput.ui.kit.rows

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * A setting picked on a continuous or stepped range: the title and the current [valueLabel] on
 * one line, the slider under them at full width. The slider is Material's, coloured by FunputUI,
 * because its drag, keyboard and accessibility handling is worth borrowing rather than rebuilding.
 */
@Composable
fun SliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueLabel: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
) {
    val colors = FunputUi.colors
    val type = FunputUi.typography
    val spacing = FunputUi.spacing
    Column(modifier.fillMaxWidth().padding(horizontal = spacing.cardPadding, vertical = spacing.medium)) {
        Row {
            BasicText(title, style = type.body.copy(color = colors.label), modifier = Modifier.weight(1f))
            valueLabel?.let { BasicText(it, style = type.body.copy(color = colors.secondaryLabel)) }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.separator,
                activeTickColor = colors.onAccent,
                inactiveTickColor = colors.secondaryLabel,
            ),
        )
    }
}
