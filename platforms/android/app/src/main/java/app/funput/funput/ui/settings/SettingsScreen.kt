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
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    keyboardSetupStatus: KeyboardSetupStatus,
    keyboardTheme: KeyboardThemeDescriptor,
    inputMethod: KeyboardInputMethod,
    showsNumberRow: Boolean,
    toneStyle: ToneStyle,
    keySizeProfile: KeyboardSizingProfile,
    hapticsEnabled: Boolean,
    soundsEnabled: Boolean,
    smartRestoreEnabled: Boolean,
    spellCheckEnabled: Boolean,
    personalSuggestionsEnabled: Boolean,
    clipboardPreferences: ClipboardPreferences,
    smartGesturesEnabled: Boolean,
    onInputMethodSelected: (KeyboardInputMethod) -> Unit,
    onShowsNumberRowChanged: (Boolean) -> Unit,
    onOpenAppearance: () -> Unit,
    onToneStyleSelected: (ToneStyle) -> Unit,
    onKeySizeSelected: (KeyboardSizingProfile) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onSoundsChanged: (Boolean) -> Unit,
    onSmartGesturesChanged: (Boolean) -> Unit,
    onSmartRestoreChanged: (Boolean) -> Unit,
    onSpellCheckChanged: (Boolean) -> Unit,
    onPersonalSuggestionsChanged: (Boolean) -> Unit,
    onClipboardEnabledChanged: (Boolean) -> Unit,
    onClipboardExpirySelected: (ClipboardExpiry) -> Unit,
    onClearClipboardHistory: () -> Unit,
    onResetPersonalSuggestions: () -> Unit,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit,
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
            keyboardTheme = keyboardTheme,
            inputMethod = inputMethod,
            showsNumberRow = showsNumberRow,
            toneStyle = toneStyle,
            keySizeProfile = keySizeProfile,
            hapticsEnabled = hapticsEnabled,
            soundsEnabled = soundsEnabled,
            smartRestoreEnabled = smartRestoreEnabled,
            spellCheckEnabled = spellCheckEnabled,
            personalSuggestionsEnabled = personalSuggestionsEnabled,
            clipboardPreferences = clipboardPreferences,
            smartGesturesEnabled = smartGesturesEnabled,
            onOpenPicker = { picker = it },
            onShowsNumberRowChanged = onShowsNumberRowChanged,
            onOpenAppearance = onOpenAppearance,
            onToneStyleSelected = onToneStyleSelected,
            onKeySizeSelected = onKeySizeSelected,
            onHapticsChanged = onHapticsChanged,
            onSoundsChanged = onSoundsChanged,
            onSmartGesturesChanged = onSmartGesturesChanged,
            onSmartRestoreChanged = onSmartRestoreChanged,
            onSpellCheckChanged = onSpellCheckChanged,
            onPersonalSuggestionsChanged = onPersonalSuggestionsChanged,
            onClipboardEnabledChanged = onClipboardEnabledChanged,
            onClearClipboardHistory = onClearClipboardHistory,
            onResetPersonalSuggestions = onResetPersonalSuggestions,
            onEnableKeyboard = onEnableKeyboard,
            onSelectKeyboard = onSelectKeyboard,
        )
    }
    SettingsPickerSheet(
        picker = picker,
        inputMethod = inputMethod,
        toneStyle = toneStyle,
        clipboardExpiry = clipboardPreferences.expiry,
        onInputMethodSelected = onInputMethodSelected,
        onToneStyleSelected = onToneStyleSelected,
        onClipboardExpirySelected = onClipboardExpirySelected,
        onDismiss = { picker = null },
    )
}
