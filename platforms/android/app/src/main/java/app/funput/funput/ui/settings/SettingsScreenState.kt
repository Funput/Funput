package app.funput.funput.ui.settings

import androidx.compose.runtime.Immutable
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.placement.KeyboardPlacementMode
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import app.funput.funput.keyboard.placement.OneHandedSide
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.settings.hardware.HardwareKeyboardSectionState
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus

/** Everything the settings screen renders and every action it can dispatch. */
@Immutable
internal class SettingsScreenState(
    val keyboardSetupStatus: KeyboardSetupStatus,
    val keyboardTheme: KeyboardThemeDescriptor,
    val inputMethod: KeyboardInputMethod,
    val showsNumberRow: Boolean,
    val toneStyle: ToneStyle,
    val keySizeProfile: KeyboardSizingProfile,
    val placement: KeyboardPlacementPreferences = KeyboardPlacementPreferences.Default,
    val hapticsEnabled: Boolean,
    val soundsEnabled: Boolean,
    val smartComposition: SmartCompositionPreferences,
    val personalSuggestionsEnabled: Boolean,
    val clipboardPreferences: ClipboardPreferences,
    val smartGesturesEnabled: Boolean,
    val hardwareKeyboard: HardwareKeyboardSectionState = HardwareKeyboardSectionState.Inert,
    val onInputMethodSelected: (KeyboardInputMethod) -> Unit,
    val onShowsNumberRowChanged: (Boolean) -> Unit,
    val onOpenAppearance: () -> Unit,
    val onOpenShortcuts: () -> Unit = {},
    val onToneStyleSelected: (ToneStyle) -> Unit,
    val onKeySizeSelected: (KeyboardSizingProfile) -> Unit,
    val onPlacementModeSelected: (KeyboardPlacementMode) -> Unit = {},
    val onElevationSelected: (Float) -> Unit = {},
    val onOneHandedWidthSelected: (Float) -> Unit = {},
    val onOneHandedSideSelected: (OneHandedSide) -> Unit = {},
    val onHapticsChanged: (Boolean) -> Unit,
    val onSoundsChanged: (Boolean) -> Unit,
    val onSmartGesturesChanged: (Boolean) -> Unit,
    val onSmartRestoreChanged: (Boolean) -> Unit,
    val onSpellCheckChanged: (Boolean) -> Unit,
    val onAutoCapitalizeChanged: (Boolean) -> Unit,
    val onPersonalSuggestionsChanged: (Boolean) -> Unit,
    val onClipboardEnabledChanged: (Boolean) -> Unit,
    val onClipboardExpirySelected: (ClipboardExpiry) -> Unit,
    val onClearClipboardHistory: () -> Unit,
    val onResetPersonalSuggestions: () -> Unit,
    val onEnableKeyboard: () -> Unit,
    val onSelectKeyboard: () -> Unit,
)
