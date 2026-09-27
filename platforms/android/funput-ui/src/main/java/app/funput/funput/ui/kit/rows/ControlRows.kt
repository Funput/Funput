package app.funput.funput.ui.kit.rows

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.funput.funput.ui.kit.controls.FunputSegmented
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * A setting picked on a range: a [FunputRow] header with the current [valueLabel] as its detail,
 * then the slider at full width, then the range's end labels when given.
 *
 * The slider is Material's, coloured by FunputUI, because its drag, keyboard and accessibility
 * handling is worth borrowing. TalkBack names it after [title]. Report the final value from
 * [onValueChangeFinished] when every intermediate value would be a wasted write.
 */
@Composable
fun SliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueLabel: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    rangeLabels: Pair<String, String>? = null,
    onValueChangeFinished: (() -> Unit)? = null,
    @DrawableRes icon: Int? = null,
    tint: FunputTint = FunputTint.ORANGE,
    steps: Int = 0,
    enabled: Boolean = true,
) {
    val colors = FunputUi.colors
    val spacing = FunputUi.spacing
    Column(modifier.fillMaxWidth()) {
        FunputRow(
            title = title,
            icon = icon,
            tint = tint,
            enabled = enabled,
            detail = valueLabel?.let { label ->
                { BasicText(label, style = FunputUi.typography.body.copy(color = colors.secondaryLabel)) }
            },
        )
        Column(
            Modifier.padding(start = spacing.cardPadding, end = spacing.cardPadding, bottom = spacing.medium),
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
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
                modifier = Modifier.semantics { contentDescription = title },
            )
            rangeLabels?.let { (start, end) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val style = FunputUi.typography.caption.copy(color = colors.secondaryLabel)
                    BasicText(start, style = style)
                    BasicText(end, style = style)
                }
            }
        }
    }
}

/**
 * A setting with a few short options: a [FunputRow] header, then a [FunputSegmented] under it at
 * full width, so long Vietnamese option labels never fight the title for one line.
 */
@Composable
fun SegmentedRow(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    tint: FunputTint = FunputTint.ORANGE,
) {
    Column(modifier.fillMaxWidth()) {
        FunputRow(title = title, icon = icon, tint = tint)
        FunputSegmented(
            options = options,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            modifier = Modifier.padding(
                start = FunputUi.spacing.cardPadding,
                end = FunputUi.spacing.cardPadding,
                bottom = FunputUi.spacing.medium,
            ),
        )
    }
}
