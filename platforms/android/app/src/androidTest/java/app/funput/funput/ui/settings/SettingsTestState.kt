package app.funput.funput.ui.settings

import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.theme.InstalledThemeRepository
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus

internal fun testSettingsState(
    keyboardSetupStatus: KeyboardSetupStatus = KeyboardSetupStatus.READY,
) = SettingsScreenState(
    keyboardSetupStatus = keyboardSetupStatus,
    keyboardTheme = InstalledThemeRepository.builtIn().themes.first(),
    inputMethod = KeyboardInputMethod.TELEX,
    showsNumberRow = true,
    toneStyle = ToneStyle.TRADITIONAL,
    keySizeProfile = KeyboardSizingProfile.Normal,
    hapticsEnabled = true,
    soundsEnabled = false,
    smartComposition = SmartCompositionPreferences.Default,
    personalSuggestionsEnabled = true,
    clipboardPreferences = ClipboardPreferences.Default,
    smartGesturesEnabled = true,
    onInputMethodSelected = {},
    onShowsNumberRowChanged = {},
    onOpenAppearance = {},
    onToneStyleSelected = {},
    onKeySizeSelected = {},
    onHapticsChanged = {},
    onSoundsChanged = {},
    onSmartGesturesChanged = {},
    onSmartRestoreChanged = {},
    onSpellCheckChanged = {},
    onAutoCapitalizeChanged = {},
    onPersonalSuggestionsChanged = {},
    onClipboardEnabledChanged = {},
    onClipboardExpirySelected = {},
    onClearClipboardHistory = {},
    onResetPersonalSuggestions = {},
    onEnableKeyboard = {},
    onSelectKeyboard = {},
)
