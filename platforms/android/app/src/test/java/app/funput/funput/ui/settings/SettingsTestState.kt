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
    clipboardPreferences: ClipboardPreferences = ClipboardPreferences.Default.copy(expiry = clipboardExpiry),
    onInputMethodSelected: (KeyboardInputMethod) -> Unit = {},
    onShowsNumberRowChanged: (Boolean) -> Unit = {},
    onToneStyleSelected: (ToneStyle) -> Unit = {},
    onKeySizeSelected: (KeyboardSizingProfile) -> Unit = {},
    onSmartChanged: (String, Boolean) -> Unit = { _, _ -> },
    hapticsEnabled: Boolean = true,
    soundsEnabled: Boolean = false,
    speechAvailable: Boolean = false,
    smartComposition: SmartCompositionPreferences = SmartCompositionPreferences.Default,
    onClipboardExpirySelected: (ClipboardExpiry) -> Unit = {},
) = SettingsScreenState(
    keyboardSetupStatus = keyboardSetupStatus,
    keyboardTheme = InstalledThemeRepository.builtIn().themes.first(),
    inputMethod = inputMethod,
    showsNumberRow = showsNumberRow,
    toneStyle = ToneStyle.TRADITIONAL,
    keySizeProfile = KeyboardSizingProfile.Normal,
    placement = placement,
    hapticsEnabled = hapticsEnabled,
    soundsEnabled = soundsEnabled,
    smartComposition = smartComposition,
    personalSuggestionsEnabled = true,
    clipboardPreferences = clipboardPreferences,
    smartGesturesEnabled = true,
    onInputMethodSelected = onInputMethodSelected,
    onShowsNumberRowChanged = onShowsNumberRowChanged,
    onOpenAppearance = {},
    speechAvailable = speechAvailable,
    onToneStyleSelected = onToneStyleSelected,
    onKeySizeSelected = onKeySizeSelected,
    onHapticsChanged = {},
    onSoundsChanged = {},
    onSmartGesturesChanged = { onSmartChanged("gestures", it) },
    onSmartRestoreChanged = { onSmartChanged("restore", it) },
    onSpellCheckChanged = { onSmartChanged("spelling", it) },
    onAutoCapitalizeChanged = { onSmartChanged("capitalization", it) },
    onPersonalSuggestionsChanged = { onSmartChanged("suggestions", it) },
    onClipboardEnabledChanged = {},
    onClipboardExpirySelected = onClipboardExpirySelected,
    onClearClipboardHistory = {},
    onResetPersonalSuggestions = {},
    onReturnsToLettersChanged = { onSmartChanged("letters", it) },
    onEnableKeyboard = {},
    onSelectKeyboard = {},
)

/** Where the app's captures are written when recorded locally; see funput-ui's KIT_SCREENSHOT_ROOT. */
internal const val APP_SCREENSHOT_ROOT: String = "build/outputs/roborazzi"
