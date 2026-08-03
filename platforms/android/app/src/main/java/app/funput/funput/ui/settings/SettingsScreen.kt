package app.funput.funput.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus

@Composable
internal fun SettingsScreen(
    keyboardSetupStatus: KeyboardSetupStatus,
    inputMethod: KeyboardInputMethod,
    showsNumberRow: Boolean,
    toneStyle: ToneStyle,
    keySizeProfile: KeyboardSizingProfile,
    keyboardThemeLabel: String,
    appearanceMode: AppearanceMode,
    hapticsEnabled: Boolean,
    soundsEnabled: Boolean,
    smartRestoreEnabled: Boolean,
    spellCheckEnabled: Boolean,
    personalSuggestionsEnabled: Boolean,
    versionName: String,
    onInputMethodSelected: (KeyboardInputMethod) -> Unit,
    onShowsNumberRowChanged: (Boolean) -> Unit,
    onToneStyleSelected: (ToneStyle) -> Unit,
    onKeySizeSelected: (KeyboardSizingProfile) -> Unit,
    onAppearanceSelected: (AppearanceMode) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onSoundsChanged: (Boolean) -> Unit,
    onSmartRestoreChanged: (Boolean) -> Unit,
    onSpellCheckChanged: (Boolean) -> Unit,
    onPersonalSuggestionsChanged: (Boolean) -> Unit,
    onResetPersonalSuggestions: () -> Unit,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit,
    onOpenThemeGallery: () -> Unit,
    onOpenWebsite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var picker by rememberSaveable { mutableStateOf<SettingsPicker?>(null) }
    Box(modifier = modifier.fillMaxSize()) {
        SettingsScreenSections(
            keyboardSetupStatus = keyboardSetupStatus,
            inputMethod = inputMethod,
            showsNumberRow = showsNumberRow,
            toneStyle = toneStyle,
            keySizeProfile = keySizeProfile,
            keyboardThemeLabel = keyboardThemeLabel,
            appearanceMode = appearanceMode,
            hapticsEnabled = hapticsEnabled,
            soundsEnabled = soundsEnabled,
            smartRestoreEnabled = smartRestoreEnabled,
            spellCheckEnabled = spellCheckEnabled,
            personalSuggestionsEnabled = personalSuggestionsEnabled,
            versionName = versionName,
            onOpenPicker = { picker = it },
            onShowsNumberRowChanged = onShowsNumberRowChanged,
            onHapticsChanged = onHapticsChanged,
            onSoundsChanged = onSoundsChanged,
            onSmartRestoreChanged = onSmartRestoreChanged,
            onSpellCheckChanged = onSpellCheckChanged,
            onPersonalSuggestionsChanged = onPersonalSuggestionsChanged,
            onResetPersonalSuggestions = onResetPersonalSuggestions,
            onEnableKeyboard = onEnableKeyboard,
            onSelectKeyboard = onSelectKeyboard,
            onOpenThemeGallery = onOpenThemeGallery,
            onOpenWebsite = onOpenWebsite,
        )
    }
    SettingsPickerSheet(
        picker = picker,
        inputMethod = inputMethod,
        toneStyle = toneStyle,
        keySizeProfile = keySizeProfile,
        appearanceMode = appearanceMode,
        onInputMethodSelected = onInputMethodSelected,
        onToneStyleSelected = onToneStyleSelected,
        onKeySizeSelected = onKeySizeSelected,
        onAppearanceSelected = onAppearanceSelected,
        onDismiss = { picker = null },
    )
}
