package app.funput.funput.ui.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.funput.funput.ui.settings.clipboard.ClipboardSection
import app.funput.funput.ui.settings.data.DataSection
import app.funput.funput.ui.settings.extraonsets.ExtraOnsetsSection
import app.funput.funput.ui.settings.feedback.FeedbackSection
import app.funput.funput.ui.settings.hardware.HardwareKeyboardSection
import app.funput.funput.ui.settings.keyboard.KeyboardPreviewCard
import app.funput.funput.ui.settings.keyboard.KeyboardSetupCard
import app.funput.funput.ui.settings.keyboard.LayoutSection
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
import app.funput.funput.ui.settings.speech.SpeechSettingsEntry
import app.funput.funput.ui.settings.smart.SmartSection
import app.funput.funput.ui.settings.typing.TypingSection

/**
 * The settings page, top to bottom: the keyboard preview, setup while it is unfinished, then one
 * group per concern, each with its own icon colour.
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
    if (state.speechAvailable) item(key = "speech") { SpeechSettingsEntry(state.onOpenSpeech) }
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
        SmartSection(
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
    item(key = "extra-onsets") { ExtraOnsetsSection(state.extraOnsets) }
    item(key = "feedback") {
        FeedbackSection(
            hapticsEnabled = state.hapticsEnabled,
            soundsEnabled = state.soundsEnabled,
            onHapticsChanged = state.onHapticsChanged,
            onSoundsChanged = state.onSoundsChanged,
        )
    }
    item(key = "clipboard") {
        ClipboardSection(
            enabled = state.clipboardPreferences.enabled,
            expiry = state.clipboardPreferences.expiry,
            onEnabledChanged = state.onClipboardEnabledChanged,
            onOpenExpiry = { onOpenPicker(SettingsPicker.CLIPBOARD_EXPIRY) },
        )
    }
    item(key = "hardware-keyboard") { HardwareKeyboardSection(state.hardwareKeyboard) }
    item(key = "data") {
        DataSection(
            onResetPersonalSuggestions = state.onResetPersonalSuggestions,
            onClearClipboardHistory = state.onClearClipboardHistory,
        )
    }
}

/** Test tag of the keyboard preview card. */
internal const val SettingsHeroTag = "settings-hero"

/** Test tag of the setup card. */
internal const val SettingsSetupTag = "settings-setup"
