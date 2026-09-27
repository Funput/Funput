package app.funput.funput.ui.settings.typing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.LinkRow
import app.funput.funput.ui.kit.rows.SegmentedRow
import app.funput.funput.ui.settings.SettingsPicker
import app.funput.funput.ui.settings.label

/** How Vietnamese is typed: the input method, where tone marks go, and text shortcuts. */
@Composable
internal fun TypingSection(
    inputMethod: KeyboardInputMethod,
    toneStyle: ToneStyle,
    onOpenPicker: (SettingsPicker) -> Unit,
    onToneStyleSelected: (ToneStyle) -> Unit,
    onOpenShortcuts: () -> Unit,
) {
    FunputSection(
        title = stringResource(R.string.settings_section_typing),
        modifier = Modifier.testTag(TypingSettingsSectionTag),
    ) {
        LinkRow(
            title = stringResource(R.string.settings_input_method_title),
            value = inputMethod.label(),
            icon = FunputIcons.Keyboard,
            onClick = { onOpenPicker(SettingsPicker.INPUT_METHOD) },
        )
        FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
        SegmentedRow(
            title = stringResource(R.string.settings_tone_style_title),
            options = ToneStyle.entries.map { it.label() },
            selectedIndex = toneStyle.ordinal,
            onSelect = { onToneStyleSelected(ToneStyle.entries[it]) },
            icon = FunputIcons.ToneMarks,
        )
        FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
        LinkRow(
            title = stringResource(R.string.shortcuts_title),
            summary = stringResource(R.string.shortcuts_settings_summary),
            icon = FunputIcons.Shortcuts,
            onClick = onOpenShortcuts,
        )
    }
}

/** Test tag of the typing section. */
internal const val TypingSettingsSectionTag = "settings-section-typing"
