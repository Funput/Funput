package app.funput.funput.ui.settings.hub

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.theme.InstalledThemeRepository
import app.funput.funput.ui.kit.theme.FunputUiTheme
import app.funput.funput.ui.settings.SettingsScreenState
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus

@Preview(name = "Hub, not enabled", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotEnabledHubPreview() {
    HubPreview(KeyboardSetupStatus.NOT_ENABLED, isDark = false)
}

@Preview(name = "Hub, ready", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReadyHubPreview() {
    HubPreview(KeyboardSetupStatus.READY, isDark = false)
}

@Preview(
    name = "Hub, dark",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DarkHubPreview() {
    HubPreview(KeyboardSetupStatus.READY, isDark = true)
}

@Composable
private fun HubPreview(status: KeyboardSetupStatus, isDark: Boolean) {
    FunputUiTheme(isDark = isDark) {
        KeyboardHubScreen(state = previewSettingsState(status), onOpenPage = {})
    }
}

/** Representative settings with inert actions, shared by the hub and page previews. */
internal fun previewSettingsState(
    keyboardSetupStatus: KeyboardSetupStatus = KeyboardSetupStatus.READY,
) = SettingsScreenState(
    keyboardSetupStatus = keyboardSetupStatus,
    keyboardTheme = InstalledThemeRepository.builtIn().themes.first(),
    inputMethod = KeyboardInputMethod.VNI,
    showsNumberRow = false,
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
