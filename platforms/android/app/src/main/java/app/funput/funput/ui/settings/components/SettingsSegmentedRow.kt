package app.funput.funput.ui.settings.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.funput.funput.ui.theme.Spacing

/**
 * A short, fixed set of choices, shown rather than hidden behind a sheet.
 *
 * A row that opens a sheet to pick one of two costs three taps to read a state that fits on the
 * screen, and it makes every group of settings the same shape as every other. This is for the sets
 * whose options need no explaining; anything whose choices need a sentence each stays a row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SettingsSegmentedRow(
    position: RowPosition,
    title: String,
    @DrawableRes iconRes: Int,
    options: List<T>,
    selected: T,
    labelOf: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsRowSurface(
        position = position,
        interactionSource = remember { MutableInteractionSource() },
        modifier = modifier,
    ) {
        SettingsIcon(iconRes)
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == selected,
                        onClick = { onSelected(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        label = { Text(labelOf(option)) },
                    )
                }
            }
        }
    }
}
