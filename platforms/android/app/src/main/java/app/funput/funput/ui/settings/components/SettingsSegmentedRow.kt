package app.funput.funput.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    title: String,
    options: List<T>,
    selected: T,
    labelOf: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        modifier = modifier.fillMaxWidth(),
    ) {
        SettingsSectionHeader(title)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    label = { Text(labelOf(option)) },
                )
            }
        }
    }
}
