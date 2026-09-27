package app.funput.funput.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.overlays.FunputSheet
import app.funput.funput.ui.kit.rows.ChoiceRow

/** Shows the sheet for [picker], bound to the matching value and action in [state]; nothing when null. */
@Composable
internal fun SettingsPickerSheet(picker: SettingsPicker?, state: SettingsScreenState, onDismiss: () -> Unit) {
    when (picker) {
        SettingsPicker.INPUT_METHOD -> PickerSheet(
            title = stringResource(R.string.settings_input_method_title),
            options = inputMethodOptions(),
            selected = state.inputMethod,
            onSelected = state.onInputMethodSelected,
            onDismiss = onDismiss,
        )
        SettingsPicker.TONE_STYLE -> PickerSheet(
            title = stringResource(R.string.settings_tone_style_title),
            options = toneStyleOptions(),
            selected = state.toneStyle,
            onSelected = state.onToneStyleSelected,
            onDismiss = onDismiss,
        )
        SettingsPicker.KEYBOARD_PLACEMENT -> PickerSheet(
            title = stringResource(R.string.settings_keyboard_mode_title),
            options = keyboardPlacementOptions(),
            selected = state.placement.activeMode,
            onSelected = state.onPlacementModeSelected,
            onDismiss = onDismiss,
        )
        SettingsPicker.CLIPBOARD_EXPIRY -> PickerSheet(
            title = stringResource(R.string.settings_clipboard_expiry_title),
            options = clipboardExpiryOptions(),
            selected = state.clipboardPreferences.expiry,
            onSelected = state.onClipboardExpirySelected,
            onDismiss = onDismiss,
        )
        null -> Unit
    }
}

/**
 * A sheet of mutually exclusive [options], the current one checked. Choosing one applies it and
 * closes the sheet: there is nothing else to confirm.
 */
@Composable
internal fun <T> PickerSheet(
    title: String,
    options: List<PickerOption<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    FunputSheet(onDismiss = onDismiss, title = title) {
        FunputSection(title = null) {
            options.forEachIndexed { index, option ->
                if (index > 0) FunputDivider()
                ChoiceRow(
                    title = option.label,
                    summary = option.summary,
                    selected = option.value == selected,
                    onSelect = {
                        onSelected(option.value)
                        onDismiss()
                    },
                )
            }
        }
    }
}
