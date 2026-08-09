package app.funput.funput.ui.settings.keyboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.settings.SettingsPicker
import app.funput.funput.ui.settings.components.SettingsGroup
import app.funput.funput.ui.settings.components.SettingsIconTone
import app.funput.funput.ui.settings.components.SettingsRow
import app.funput.funput.ui.settings.components.SettingsSectionHeader
import app.funput.funput.ui.settings.components.SettingsSwitchRow
import app.funput.funput.ui.settings.label
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
import app.funput.funput.ui.theme.Spacing

@Composable
internal fun KeyboardSettingsSection(
    setupStatus: KeyboardSetupStatus,
    inputMethod: KeyboardInputMethod,
    showsNumberRow: Boolean,
    toneStyle: ToneStyle,
    keySizeProfile: KeyboardSizingProfile,
    onOpenPicker: (SettingsPicker) -> Unit,
    onShowsNumberRowChanged: (Boolean) -> Unit,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // The setup card carries its own title and is an alert rather than a section, so it sits
        // above the heading instead of under one. Two headings in a row — "Bàn phím" and then
        // "Tùy chỉnh" — were naming the same thing twice.
        KeyboardSetupCard(
            status = setupStatus,
            onEnableKeyboard = onEnableKeyboard,
            onSelectKeyboard = onSelectKeyboard,
        )
        if (setupStatus != KeyboardSetupStatus.READY) {
            Spacer(modifier = Modifier.height(Spacing.Large))
        }
        SettingsSectionHeader(stringResource(R.string.settings_section_keyboard))
        SettingsGroup(
            rows = buildList {
                add { position ->
                    SettingsRow(
                        position = position,
                        title = stringResource(R.string.settings_input_method_title),
                        value = inputMethod.label(),
                        iconRes = R.drawable.ic_keyboard,
                        tone = SettingsIconTone.Primary,
                        onClick = { onOpenPicker(SettingsPicker.INPUT_METHOD) },
                    )
                }
                // VNI types tones with the digits, so there is nothing here to decide. A switch
                // that can only ever be on is a row that only takes up space.
                if (inputMethod.isTelexFamily) {
                    add { position ->
                        SettingsSwitchRow(
                            position = position,
                            title = stringResource(R.string.settings_number_row_title),
                            summary = stringResource(R.string.settings_number_row_summary),
                            checked = showsNumberRow,
                            iconRes = R.drawable.ic_key_size,
                            tone = SettingsIconTone.Secondary,
                            onCheckedChange = onShowsNumberRowChanged,
                        )
                    }
                }
                add { position ->
                    SettingsRow(
                        position = position,
                        title = stringResource(R.string.settings_tone_style_title),
                        value = toneStyle.label(),
                        iconRes = R.drawable.ic_globe,
                        tone = SettingsIconTone.Tertiary,
                        onClick = { onOpenPicker(SettingsPicker.TONE_STYLE) },
                    )
                }
                add { position ->
                    SettingsRow(
                        position = position,
                        title = stringResource(R.string.settings_key_size_title),
                        value = keySizeProfile.label(),
                        iconRes = R.drawable.ic_key_size,
                        tone = SettingsIconTone.Secondary,
                        onClick = { onOpenPicker(SettingsPicker.KEY_SIZE) },
                    )
                }
            },
        )
    }
}
