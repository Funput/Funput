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
import app.funput.funput.ui.settings.SettingsScreenState
import app.funput.funput.ui.settings.hardware.rememberHardwareKeyboardSectionState
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
    onOpenShortcuts: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var clipboardClearJob by remember { mutableStateOf<Job?>(null) }

    SettingsScreen(
        state = SettingsScreenState(
            keyboardSetupStatus = rememberKeyboardSetupStatus(),
            keyboardTheme = keyboardTheme,
            inputMethod = settings.inputMethod,
            showsNumberRow = settings.showsNumberRow,
            toneStyle = settings.toneStyle,
            keySizeProfile = settings.keySizeProfile,
            placement = settings.placement,
            hapticsEnabled = settings.feedback.hapticsEnabled,
            soundsEnabled = settings.feedback.soundsEnabled,
            smartComposition = settings.smartComposition,
            personalSuggestionsEnabled = settings.personalSuggestions.enabled,
            clipboardPreferences = settings.clipboard,
            smartGesturesEnabled = settings.smartGesturesEnabled,
            hardwareKeyboard = rememberHardwareKeyboardSectionState(),
            onInputMethodSelected = { method ->
                scope.launch { settings.input.setInputMethod(method) }
            },
            onShowsNumberRowChanged = { enabled ->
                scope.launch { settings.numberRowStore.setShowsNumberRow(enabled) }
            },
            onOpenAppearance = onOpenAppearance,
            onOpenShortcuts = onOpenShortcuts,
            onToneStyleSelected = { style ->
                scope.launch { settings.toneStyleStore.setToneStyle(style) }
            },
            onKeySizeSelected = { profile ->
                scope.launch { settings.sizing.setProfile(profile) }
            },
            onPlacementModeSelected = { mode ->
                scope.launch { settings.placementStore.setMode(mode) }
            },
            onElevationSelected = { offsetDp ->
                scope.launch { settings.placementStore.setElevatedOffsetDp(offsetDp) }
            },
            onOneHandedWidthSelected = { width ->
                scope.launch { settings.placementStore.setOneHandedWidthFraction(width) }
            },
            onOneHandedSideSelected = { side ->
                scope.launch { settings.placementStore.setOneHandedSide(side) }
            },
            onHapticsChanged = { enabled ->
                scope.launch { settings.feedbackStore.setHapticsEnabled(enabled) }
            },
            onSoundsChanged = { enabled ->
                scope.launch { settings.feedbackStore.setSoundsEnabled(enabled) }
            },
            onSmartGesturesChanged = { enabled ->
                scope.launch { settings.smartGestureStore.setEnabled(enabled) }
            },
            onSmartRestoreChanged = { enabled ->
                scope.launch { settings.smartCompositionStore.setSmartRestoreEnabled(enabled) }
            },
            onSpellCheckChanged = { enabled ->
                scope.launch { settings.smartCompositionStore.setSpellCheckEnabled(enabled) }
            },
            onAutoCapitalizeChanged = { enabled ->
                scope.launch { settings.smartCompositionStore.setAutoCapitalizeEnabled(enabled) }
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
        ),
    )
}
