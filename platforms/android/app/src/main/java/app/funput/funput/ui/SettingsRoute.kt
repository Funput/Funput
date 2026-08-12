package app.funput.funput.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.funput.funput.ime.clipboard.persistence.ClipboardHistoryStore
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.keyboard.openKeyboardSettings
import app.funput.funput.ui.keyboard.showKeyboardPicker
import app.funput.funput.ui.settings.SettingsScreen
import app.funput.funput.ui.settings.setup.rememberKeyboardSetupStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Binds the settings screen to the persisted settings.
 *
 * Every control here is a straight read-then-write against one store, so keeping the wiring out
 * of the navigation host leaves that file about routing.
 */
@Composable
internal fun SettingsRoute(
    settings: FunputSettingsState,
    keyboardTheme: KeyboardThemeDescriptor,
    onOpenAppearance: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var clipboardClearJob by remember { mutableStateOf<Job?>(null) }

    SettingsScreen(
        keyboardSetupStatus = rememberKeyboardSetupStatus(),
        keyboardTheme = keyboardTheme,
        onOpenAppearance = onOpenAppearance,
        inputMethod = settings.inputMethod,
        showsNumberRow = settings.showsNumberRow,
        toneStyle = settings.toneStyle,
        keySizeProfile = settings.keySizeProfile,
        hapticsEnabled = settings.feedback.hapticsEnabled,
        soundsEnabled = settings.feedback.soundsEnabled,
        smartRestoreEnabled = settings.smartComposition.smartRestoreEnabled,
        spellCheckEnabled = settings.smartComposition.spellCheckEnabled,
        personalSuggestionsEnabled = settings.personalSuggestions.enabled,
        clipboardPreferences = settings.clipboard,
        onInputMethodSelected = { method -> scope.launch { settings.input.setInputMethod(method) } },
        onShowsNumberRowChanged = { enabled ->
            scope.launch { settings.numberRowStore.setShowsNumberRow(enabled) }
        },
        onToneStyleSelected = { style -> scope.launch { settings.toneStyleStore.setToneStyle(style) } },
        onKeySizeSelected = { profile -> scope.launch { settings.sizing.setProfile(profile) } },
        onHapticsChanged = { enabled ->
            scope.launch { settings.feedbackStore.setHapticsEnabled(enabled) }
        },
        onSoundsChanged = { enabled ->
            scope.launch { settings.feedbackStore.setSoundsEnabled(enabled) }
        },
        onSmartRestoreChanged = { enabled ->
            scope.launch { settings.smartCompositionStore.setSmartRestoreEnabled(enabled) }
        },
        onSpellCheckChanged = { enabled ->
            scope.launch { settings.smartCompositionStore.setSpellCheckEnabled(enabled) }
        },
        onPersonalSuggestionsChanged = { enabled ->
            scope.launch { settings.personalSuggestionStore.setEnabled(enabled) }
        },
        onClipboardEnabledChanged = { enabled ->
            scope.launch { settings.clipboardStore.setEnabled(enabled) }
        },
        onClipboardExpirySelected = { expiry ->
            scope.launch { settings.clipboardStore.setExpiry(expiry) }
        },
        onClearClipboardHistory = {
            if (clipboardClearJob?.isActive != true) {
                clipboardClearJob = scope.launch {
                    withContext(Dispatchers.IO) { ClipboardHistoryStore.from(context).clear() }
                }
            }
        },
        onResetPersonalSuggestions = {
            scope.launch { settings.personalSuggestionStore.requestReset() }
        },
        onEnableKeyboard = context::openKeyboardSettings,
        onSelectKeyboard = context::showKeyboardPicker,
    )
}
