package app.funput.funput.ui.settings.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import app.funput.funput.ime.clipboard.persistence.ClipboardHistoryStore
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.FunputSettingsState
import app.funput.funput.ui.keyboard.openKeyboardSettings
import app.funput.funput.ui.keyboard.showKeyboardPicker
import app.funput.funput.ui.settings.SettingsScreenState
import app.funput.funput.ui.settings.extraonsets.rememberExtraOnsetsSectionState
import app.funput.funput.ui.settings.hardware.rememberHardwareKeyboardSectionState
import app.funput.funput.ui.settings.setup.rememberKeyboardSetupStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Binds the settings to the persisted stores: every value the hub and its pages show, and every
 * action they dispatch. Each settings destination calls it for itself, all reading the one shared
 * [FunputSettingsState], so the hub and "Tôi" can never hold two diverging copies.
 *
 * Every control here is a straight read-then-write against one store, so keeping the wiring out
 * of the navigation host leaves that file about routing.
 */
@Composable
internal fun rememberSettingsScreenState(
    settings: FunputSettingsState,
    keyboardTheme: KeyboardThemeDescriptor,
    onOpenAppearance: () -> Unit,
): SettingsScreenState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var clipboardClearJob by remember { mutableStateOf<Job?>(null) }

    return SettingsScreenState(
        keyboardSetupStatus = rememberKeyboardSetupStatus(),
        keyboardTheme = keyboardTheme,
        inputMethod = settings.inputMethod,
        showsNumberRow = settings.showsNumberRow,
        toneStyle = settings.toneStyle,
        keySizeProfile = settings.keySizeProfile,
        placement = settings.placement,
        hapticsEnabled = settings.feedback.hapticsEnabled,
        hapticStrength = settings.feedback.hapticStrength,
        soundsEnabled = settings.feedback.soundsEnabled,
        keyPreviewsEnabled = settings.feedback.keyPreviewsEnabled,
        smartComposition = settings.smartComposition,
        personalSuggestionsEnabled = settings.personalSuggestions.enabled,
        clipboardPreferences = settings.clipboard,
        smartGesturesEnabled = settings.smartGesturesEnabled,
        returnsToLettersEnabled = settings.returnsToLettersEnabled,
        hardwareKeyboard = rememberHardwareKeyboardSectionState(),
        extraOnsets = rememberExtraOnsetsSectionState(settings.smartComposition.extraOnsets, settings.inputMethod),
        onInputMethodSelected = { method ->
            scope.launch { settings.input.setInputMethod(method) }
        },
        onShowsNumberRowChanged = { enabled ->
            scope.launch { settings.numberRowStore.setShowsNumberRow(enabled) }
        },
        onOpenAppearance = onOpenAppearance,
        speechAvailable = LocalResources.current.getBoolean(app.funput.funput.ime.R.bool.speech_feature_available),
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
        onHapticStrengthSelected = { strength ->
            scope.launch { settings.feedbackStore.selectHaptics(strength) }
        },
        onKeyPreviewsChanged = { enabled ->
            scope.launch { settings.feedbackStore.setKeyPreviewsEnabled(enabled) }
        },
        onSoundsChanged = { enabled ->
            scope.launch { settings.feedbackStore.setSoundsEnabled(enabled) }
        },
        onSmartGesturesChanged = { enabled ->
            scope.launch { settings.smartGestureStore.setEnabled(enabled) }
        },
        onReturnsToLettersChanged = { enabled ->
            scope.launch { settings.letterReturnStore.setEnabled(enabled) }
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
    )
}
