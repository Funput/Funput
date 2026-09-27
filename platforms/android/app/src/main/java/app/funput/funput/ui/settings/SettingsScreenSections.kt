package app.funput.funput.ui.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.funput.funput.ui.settings.clipboard.ClipboardSettingsSection
import app.funput.funput.ui.settings.data.DataSettingsSection
import app.funput.funput.ui.settings.feedback.FeedbackSettingsSection
import app.funput.funput.ui.settings.hardware.HardwareKeyboardSettingsSection
import app.funput.funput.ui.settings.keyboard.KeyboardPreviewCard
import app.funput.funput.ui.settings.keyboard.KeyboardSetupCard
import app.funput.funput.ui.settings.keyboard.LayoutSection
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
import app.funput.funput.ui.settings.smart.SmartSettingsSection
import app.funput.funput.ui.settings.typing.TypingSection

/**
 * The settings page, top to bottom: the keyboard preview, setup while it is unfinished, then one
 * group per concern. The groups from Smart input down are still the pre-FunputUI sections; they
 * read their colours from the FunputUI Material bridge until they are rebuilt.
 */
internal fun LazyListScope.settingsSections(state: SettingsScreenState, onOpenPicker: (SettingsPicker) -> Unit) {
    item(key = "preview") {
        KeyboardPreviewCard(
            descriptor = state.keyboardTheme,
            inputMethod = state.inputMethod,
            sizingProfile = state.keySizeProfile,
            showsNumberRow = state.showsNumberRow,
            onOpenAppearance = state.onOpenAppearance,
            modifier = Modifier.testTag(SettingsHeroTag),
        )
    }
    if (state.keyboardSetupStatus != KeyboardSetupStatus.READY) {
        item(key = "setup") {
            KeyboardSetupCard(
                status = state.keyboardSetupStatus,
                onEnableKeyboard = state.onEnableKeyboard,
                onSelectKeyboard = state.onSelectKeyboard,
                modifier = Modifier.testTag(SettingsSetupTag),
            )
        }
    }
    item(key = "typing") {
        TypingSection(
            inputMethod = state.inputMethod,
            toneStyle = state.toneStyle,
            onOpenPicker = onOpenPicker,
            onToneStyleSelected = state.onToneStyleSelected,
            onOpenShortcuts = state.onOpenShortcuts,
        )
    }
    item(key = "layout") {
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
    item(key = "smart") {
        SmartSettingsSection(
            preferences = state.smartComposition,
            personalSuggestionsEnabled = state.personalSuggestionsEnabled,
            smartGesturesEnabled = state.smartGesturesEnabled,
            returnsToLettersEnabled = state.returnsToLettersEnabled,
            onSmartRestoreChanged = state.onSmartRestoreChanged,
            onSpellCheckChanged = state.onSpellCheckChanged,
            onAutoCapitalizeChanged = state.onAutoCapitalizeChanged,
            onPersonalSuggestionsChanged = state.onPersonalSuggestionsChanged,
            onSmartGesturesChanged = state.onSmartGesturesChanged,
            onReturnsToLettersChanged = state.onReturnsToLettersChanged,
        )
    }
    item(key = "feedback") {
        FeedbackSettingsSection(
            hapticsEnabled = state.hapticsEnabled,
            soundsEnabled = state.soundsEnabled,
            onHapticsChanged = state.onHapticsChanged,
            onSoundsChanged = state.onSoundsChanged,
        )
    }
    item(key = "clipboard") {
        ClipboardSettingsSection(
            enabled = state.clipboardPreferences.enabled,
            expiry = state.clipboardPreferences.expiry,
            onEnabledChanged = state.onClipboardEnabledChanged,
            onOpenExpiry = { onOpenPicker(SettingsPicker.CLIPBOARD_EXPIRY) },
        )
    }
    item(key = "hardware-keyboard") { HardwareKeyboardSettingsSection(state.hardwareKeyboard) }
    item(key = "data") {
        DataSettingsSection(
            onResetPersonalSuggestions = state.onResetPersonalSuggestions,
            onClearClipboardHistory = state.onClearClipboardHistory,
        )
    }
}

/** Test tag of the keyboard preview card. */
internal const val SettingsHeroTag = "settings-hero"

/** Test tag of the setup card. */
internal const val SettingsSetupTag = "settings-setup"
