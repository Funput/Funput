package app.funput.funput.ui.settings.typing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.settings.SettingsPicker
import app.funput.funput.ui.settings.components.SettingsRow
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.components.SettingsSegmentedRow
import app.funput.funput.ui.settings.label

@Composable
internal fun TypingSettingsSection(
    inputMethod: KeyboardInputMethod,
    toneStyle: ToneStyle,
    onOpenPicker: (SettingsPicker) -> Unit,
    onToneStyleSelected: (ToneStyle) -> Unit,
) {
    SettingsSection(
        title = stringResource(R.string.settings_section_typing),
        rows = listOf(
            { position ->
                SettingsRow(
                    position = position,
                    title = stringResource(R.string.settings_input_method_title),
                    value = inputMethod.label(),
                    iconRes = R.drawable.ic_keyboard,
                    onClick = { onOpenPicker(SettingsPicker.INPUT_METHOD) },
                )
            },
            { position ->
                SettingsSegmentedRow(
                    position = position,
                    title = stringResource(R.string.settings_tone_style_title),
                    iconRes = R.drawable.ic_globe,
                    options = ToneStyle.entries,
                    selected = toneStyle,
                    labelOf = { style -> style.label() },
                    onSelected = onToneStyleSelected,
                )
            },
        ),
        modifier = Modifier.testTag(TypingSettingsSectionTag),
    )
}

internal const val TypingSettingsSectionTag = "settings-section-typing"
