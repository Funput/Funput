package app.funput.funput.ui.appearance

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ui.settings.components.SettingsGroup
import app.funput.funput.ui.settings.components.SettingsSectionHeader
import app.funput.funput.ui.settings.components.SettingsSwitchRow
import app.funput.funput.ui.settings.label
import app.funput.funput.ui.theme.BrandOrange
import app.funput.funput.ui.theme.supportsDynamicColor

/**
 * How the app itself is coloured. Kept under its own heading, and phrased about the app, because
 * the keyboard's own light/dark setting sits on the same screen and the two are easy to read as
 * one — they are both about light and dark, and only the subject tells them apart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppAppearanceSection(
    appearanceMode: AppearanceMode,
    dynamicColorEnabled: Boolean,
    onAppearanceSelected: (AppearanceMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier.fillMaxWidth()) {
        SettingsSectionHeader(stringResource(R.string.appearance_section_app))
        // Three options, always the same three: a segmented row shows the state and changes it in
        // one tap, where a row that opens a sheet takes three and shows the state only as text.
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            AppearanceMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == appearanceMode,
                    onClick = { onAppearanceSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, AppearanceMode.entries.size),
                    label = { Text(mode.label()) },
                )
            }
        }
        if (supportsDynamicColor) {
            SettingsGroup(
                rows = listOf { position ->
                    SettingsSwitchRow(
                        position = position,
                        title = stringResource(R.string.settings_dynamic_color_title),
                        summary = stringResource(R.string.settings_dynamic_color_summary),
                        checked = dynamicColorEnabled,
                        iconRes = R.drawable.ic_dynamic_color,
                        iconBackground = BrandOrange,
                        onCheckedChange = onDynamicColorChanged,
                    )
                },
            )
        }
    }
}
