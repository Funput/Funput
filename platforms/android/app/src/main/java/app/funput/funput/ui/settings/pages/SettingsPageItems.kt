package app.funput.funput.ui.settings.pages

import androidx.compose.foundation.lazy.LazyListScope
import app.funput.funput.ui.settings.SettingsPicker
import app.funput.funput.ui.settings.SettingsScreenState
import app.funput.funput.ui.settings.clipboard.ClipboardSection
import app.funput.funput.ui.settings.data.DataSection
import app.funput.funput.ui.settings.extraonsets.ExtraOnsetsSection
import app.funput.funput.ui.settings.feedback.FeedbackSection
import app.funput.funput.ui.settings.hardware.HardwareKeyboardSection
import app.funput.funput.ui.settings.keyboard.LayoutSection
import app.funput.funput.ui.settings.smart.SmartSection
import app.funput.funput.ui.settings.typing.TypingSection

/**
 * What each page shows: the sections that used to sit in one long list, now one page per concern.
 * The set of items for a page never changes while it is open.
 */
internal fun LazyListScope.settingsPageItems(
    page: SettingsPage,
    state: SettingsScreenState,
    onOpenPicker: (SettingsPicker) -> Unit,
) {
    when (page) {
        SettingsPage.TYPING -> {
            item(key = "typing") {
                TypingSection(
                    inputMethod = state.inputMethod,
                    toneStyle = state.toneStyle,
                    onOpenPicker = onOpenPicker,
                    onToneStyleSelected = state.onToneStyleSelected,
                )
            }
            item(key = "extra-onsets") { ExtraOnsetsSection(state.extraOnsets) }
        }
        SettingsPage.LAYOUT -> item(key = "layout") {
            LayoutSection(
                inputMethod = state.inputMethod,
                showsNumberRow = state.showsNumberRow,
                keySizeProfile = state.keySizeProfile,
                placement = state.placement,
                onShowsNumberRowChanged = state.onShowsNumberRowChanged,
                onKeySizeSelected = state.onKeySizeSelected,
                onOpenPlacement = { onOpenPicker(SettingsPicker.KEYBOARD_PLACEMENT) },
                onElevationSelected = state.onElevationSelected,
                onOneHandedWidthSelected = state.onOneHandedWidthSelected,
                onOneHandedSideSelected = state.onOneHandedSideSelected,
            )
        }
        SettingsPage.SMART_INPUT -> item(key = "smart") { SmartSection(state) }
        SettingsPage.FEEDBACK -> item(key = "feedback") {
            FeedbackSection(
                hapticStrength = state.activeHapticStrength,
                soundsEnabled = state.soundsEnabled,
                onOpenHapticStrength = { onOpenPicker(SettingsPicker.HAPTIC_STRENGTH) },
                onSoundsChanged = state.onSoundsChanged,
            )
        }
        SettingsPage.CLIPBOARD -> item(key = "clipboard") {
            ClipboardSection(
                enabled = state.clipboardPreferences.enabled,
                expiry = state.clipboardPreferences.expiry,
                onEnabledChanged = state.onClipboardEnabledChanged,
                onOpenExpiry = { onOpenPicker(SettingsPicker.CLIPBOARD_EXPIRY) },
            )
        }
        SettingsPage.HARDWARE_KEYBOARD -> item(key = "hardware-keyboard") {
            HardwareKeyboardSection(state.hardwareKeyboard)
        }
        SettingsPage.DATA -> item(key = "data") {
            DataSection(
                onResetPersonalSuggestions = state.onResetPersonalSuggestions,
                onClearClipboardHistory = state.onClearClipboardHistory,
            )
        }
        // Voice input has its own screen with its own model; it is not drawn from these sections.
        SettingsPage.SPEECH -> Unit
    }
}
