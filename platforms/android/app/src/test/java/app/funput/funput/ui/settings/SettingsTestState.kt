package app.funput.funput.ui.settings

import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import app.funput.funput.theme.InstalledThemeRepository
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus

/**
 * A settings state with inert actions, for tests; each test overrides only what it looks at, so
 * a failing assertion points at one setting.
 */
internal fun testSettingsState(
    keyboardSetupStatus: KeyboardSetupStatus = KeyboardSetupStatus.READY,
    inputMethod: KeyboardInputMethod = KeyboardInputMethod.TELEX,
    showsNumberRow: Boolean = true,
    placement: KeyboardPlacementPreferences = KeyboardPlacementPreferences.Default,
    clipboardExpiry: ClipboardExpiry = ClipboardPreferences.Default.expiry,
    onInputMethodSelected: (KeyboardInputMethod) -> Unit = {},
    onShowsNumberRowChanged: (Boolean) -> Unit = {},
    onToneStyleSelected: (ToneStyle) -> Unit = {},
    onKeySizeSelected: (KeyboardSizingProfile) -> Unit = {},
    onOpenShortcuts: () -> Unit = {},
    onClipboardExpirySelected: (ClipboardExpiry) -> Unit = {},
) = SettingsScreenState(
    keyboardSetupStatus = keyboardSetupStatus,
    keyboardTheme = InstalledThemeRepository.builtIn().themes.first(),
    inputMethod = inputMethod,
    showsNumberRow = showsNumberRow,
    toneStyle = ToneStyle.TRADITIONAL,
    keySizeProfile = KeyboardSizingProfile.Normal,
    placement = placement,
    hapticsEnabled = true,
    soundsEnabled = false,
    smartComposition = SmartCompositionPreferences.Default,
    personalSuggestionsEnabled = true,
    clipboardPreferences = ClipboardPreferences.Default.copy(expiry = clipboardExpiry),
    smartGesturesEnabled = true,
    onInputMethodSelected = onInputMethodSelected,
    onShowsNumberRowChanged = onShowsNumberRowChanged,
    onOpenAppearance = {},
    onOpenShortcuts = onOpenShortcuts,
    onToneStyleSelected = onToneStyleSelected,
    onKeySizeSelected = onKeySizeSelected,
    onHapticsChanged = {},
    onSoundsChanged = {},
    onSmartGesturesChanged = {},
    onSmartRestoreChanged = {},
    onSpellCheckChanged = {},
    onAutoCapitalizeChanged = {},
    onPersonalSuggestionsChanged = {},
    onClipboardEnabledChanged = {},
    onClipboardExpirySelected = onClipboardExpirySelected,
    onClearClipboardHistory = {},
    onResetPersonalSuggestions = {},
    onEnableKeyboard = {},
    onSelectKeyboard = {},
)
