package app.funput.funput.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    keyboardSetupStatus: KeyboardSetupStatus,
    inputMethod: KeyboardInputMethod,
    showsNumberRow: Boolean,
    toneStyle: ToneStyle,
    keySizeProfile: KeyboardSizingProfile,
    keyboardThemeLabel: String,
    hapticsEnabled: Boolean,
    soundsEnabled: Boolean,
    smartRestoreEnabled: Boolean,
    spellCheckEnabled: Boolean,
    personalSuggestionsEnabled: Boolean,
    onInputMethodSelected: (KeyboardInputMethod) -> Unit,
    onShowsNumberRowChanged: (Boolean) -> Unit,
    onToneStyleSelected: (ToneStyle) -> Unit,
    onKeySizeSelected: (KeyboardSizingProfile) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onSoundsChanged: (Boolean) -> Unit,
    onSmartRestoreChanged: (Boolean) -> Unit,
    onSpellCheckChanged: (Boolean) -> Unit,
    onPersonalSuggestionsChanged: (Boolean) -> Unit,
    onResetPersonalSuggestions: () -> Unit,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit,
    onOpenThemeGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var picker by rememberSaveable { mutableStateOf<SettingsPicker?>(null) }
    val scrollBehavior = rememberSettingsScrollBehavior()
    Scaffold(
        topBar = { SettingsTopBar(scrollBehavior) },
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { contentPadding ->
        SettingsScreenSections(
            contentPadding = contentPadding,
            keyboardSetupStatus = keyboardSetupStatus,
            inputMethod = inputMethod,
            showsNumberRow = showsNumberRow,
            toneStyle = toneStyle,
            keySizeProfile = keySizeProfile,
            keyboardThemeLabel = keyboardThemeLabel,
            hapticsEnabled = hapticsEnabled,
            soundsEnabled = soundsEnabled,
            smartRestoreEnabled = smartRestoreEnabled,
            spellCheckEnabled = spellCheckEnabled,
            personalSuggestionsEnabled = personalSuggestionsEnabled,
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
        )
    }
    SettingsPickerSheet(
        picker = picker,
        inputMethod = inputMethod,
        toneStyle = toneStyle,
        keySizeProfile = keySizeProfile,
        onInputMethodSelected = onInputMethodSelected,
        onToneStyleSelected = onToneStyleSelected,
        onKeySizeSelected = onKeySizeSelected,
        onDismiss = { picker = null },
    )
}
