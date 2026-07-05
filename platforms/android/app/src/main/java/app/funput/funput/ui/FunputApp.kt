package app.funput.funput.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.funput.funput.R
import app.funput.funput.ime.settings.AppearanceSettings
import app.funput.funput.ime.settings.InputMethodSettings
import app.funput.funput.ime.settings.KeyboardFeedbackPreferences
import app.funput.funput.ime.settings.KeyboardFeedbackSettings
import app.funput.funput.ime.settings.KeyboardSizingSettings
import app.funput.funput.ime.settings.KeyboardThemeSettings
import app.funput.funput.ui.settings.SettingsScreen
import app.funput.funput.ui.theme.FunputTheme
import app.funput.funput.ui.theme.resolveDarkTheme
import kotlinx.coroutines.launch

@Composable
fun FunputApp() {
    val context = LocalContext.current
    val inputSettings = remember(context) { InputMethodSettings(context) }
    val sizingSettings = remember(context) { KeyboardSizingSettings(context) }
    val keyboardThemeSettings = remember(context) { KeyboardThemeSettings(context) }
    val appearanceSettings = remember(context) { AppearanceSettings(context) }
    val feedbackSettings = remember(context) { KeyboardFeedbackSettings(context) }
    val versionName = remember(context) { AppVersionProvider.versionName(context) }
    val inputMethod by inputSettings.inputMethod.collectAsState(InputMethodSettings.DefaultInputMethod)
    val keySizeProfile by sizingSettings.profile.collectAsState(KeyboardSizingSettings.DefaultProfile)
    val keyboardThemeId by keyboardThemeSettings.themeId.collectAsState(KeyboardThemeSettings.DefaultThemeId)
    val appearanceMode by appearanceSettings.mode.collectAsState(AppearanceSettings.DefaultMode)
    val feedback by feedbackSettings.preferences.collectAsState(KeyboardFeedbackPreferences.Default)
    val scope = rememberCoroutineScope()
    val darkTheme = appearanceMode.resolveDarkTheme(isSystemInDarkTheme())

    FunputTheme(appearanceMode = appearanceMode) {
        SyncSystemBarAppearance(darkTheme = darkTheme)
        Surface(modifier = Modifier.fillMaxSize()) {
            SettingsScreen(
                inputMethod = inputMethod,
                keySizeProfile = keySizeProfile,
                keyboardThemeId = keyboardThemeId,
                appearanceMode = appearanceMode,
                hapticsEnabled = feedback.hapticsEnabled,
                soundsEnabled = feedback.soundsEnabled,
                versionName = versionName,
                onInputMethodSelected = { method ->
                    scope.launch { inputSettings.setInputMethod(method) }
                },
                onKeySizeSelected = { profile ->
                    scope.launch { sizingSettings.setProfile(profile) }
                },
                onKeyboardThemeSelected = { themeId ->
                    scope.launch { keyboardThemeSettings.setTheme(themeId) }
                },
                onAppearanceSelected = { mode ->
                    scope.launch { appearanceSettings.setMode(mode) }
                },
                onHapticsChanged = { enabled ->
                    scope.launch { feedbackSettings.setHapticsEnabled(enabled) }
                },
                onSoundsChanged = { enabled ->
                    scope.launch { feedbackSettings.setSoundsEnabled(enabled) }
                },
                onOpenKeyboardSettings = context::openKeyboardSettings,
                onShowKeyboardPicker = context::showKeyboardPicker,
                onOpenWebsite = {
                    context.openWebsite(context.getString(R.string.settings_website_url))
                },
            )
        }
    }
}
