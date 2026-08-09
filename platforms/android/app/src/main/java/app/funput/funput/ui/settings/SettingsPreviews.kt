package app.funput.funput.ui.settings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
import app.funput.funput.ui.theme.FunputTheme

@Preview(name = "Setup · Not enabled", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotEnabledSetupPreview() {
    SettingsPreview(keyboardSetupStatus = KeyboardSetupStatus.NOT_ENABLED)
}

@Preview(name = "Setup · Not selected", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotSelectedSetupPreview() {
    SettingsPreview(keyboardSetupStatus = KeyboardSetupStatus.NOT_SELECTED)
}

@Preview(name = "Setup · Ready", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ReadySetupPreview() {
    SettingsPreview(keyboardSetupStatus = KeyboardSetupStatus.READY)
}

@Preview(
    name = "Settings · Dark",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DarkSettingsPreview() {
    SettingsPreview(
        appearanceMode = AppearanceMode.DARK,
        keyboardSetupStatus = KeyboardSetupStatus.READY,
    )
}

@Composable
private fun SettingsPreview(
    appearanceMode: AppearanceMode = AppearanceMode.LIGHT,
    keyboardSetupStatus: KeyboardSetupStatus = KeyboardSetupStatus.NOT_ENABLED,
) {
    FunputTheme(appearanceMode = appearanceMode) {
        SettingsScreen(
            keyboardSetupStatus = keyboardSetupStatus,
            inputMethod = KeyboardInputMethod.VNI,
            showsNumberRow = false,
            toneStyle = ToneStyle.TRADITIONAL,
            keySizeProfile = KeyboardSizingProfile.Normal,
            keyboardThemeLabel = "Tối",
            appearanceMode = appearanceMode,
            dynamicColorEnabled = true,
            hapticsEnabled = true,
            soundsEnabled = false,
            smartRestoreEnabled = true,
            spellCheckEnabled = false,
            personalSuggestionsEnabled = true,
            onInputMethodSelected = {},
            onShowsNumberRowChanged = {},
            onToneStyleSelected = {},
            onKeySizeSelected = {},
            onAppearanceSelected = {},
            onDynamicColorChanged = {},
            onHapticsChanged = {},
            onSoundsChanged = {},
            onSmartRestoreChanged = {},
            onSpellCheckChanged = {},
            onPersonalSuggestionsChanged = {},
            onResetPersonalSuggestions = {},
            onEnableKeyboard = {},
            onSelectKeyboard = {},
            onOpenThemeGallery = {},
        )
    }
}
