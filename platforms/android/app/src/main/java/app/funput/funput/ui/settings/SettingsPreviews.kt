package app.funput.funput.ui.settings

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.theme.FunputTheme

@Preview(name = "Settings · Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LightSettingsPreview() {
    SettingsPreview(appearanceMode = AppearanceMode.LIGHT)
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
    SettingsPreview(appearanceMode = AppearanceMode.DARK)
}

@Composable
private fun SettingsPreview(appearanceMode: AppearanceMode) {
    FunputTheme(appearanceMode = appearanceMode) {
        SettingsScreen(
            inputMethod = KeyboardInputMethod.VNI,
            keySizeProfile = KeyboardSizingProfile.Normal,
            keyboardThemeId = KeyboardThemeId.DARK,
            appearanceMode = appearanceMode,
            hapticsEnabled = true,
            soundsEnabled = false,
            versionName = "1.2026.1",
            onInputMethodSelected = {},
            onKeySizeSelected = {},
            onKeyboardThemeSelected = {},
            onAppearanceSelected = {},
            onHapticsChanged = {},
            onSoundsChanged = {},
            onOpenKeyboardSettings = {},
            onShowKeyboardPicker = {},
        )
    }
}
