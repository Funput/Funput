package app.funput.funput.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.settings.about.AboutSettingsSection
import app.funput.funput.ui.settings.appearance.AppearanceSettingsSection
import app.funput.funput.ui.settings.components.SettingsHero
import app.funput.funput.ui.settings.feedback.FeedbackSettingsSection
import app.funput.funput.ui.settings.keyboard.KeyboardSettingsSection
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus

@Composable
internal fun SettingsScreen(
    keyboardSetupStatus: KeyboardSetupStatus,
    inputMethod: KeyboardInputMethod,
    keySizeProfile: KeyboardSizingProfile,
    keyboardThemeId: KeyboardThemeId,
    appearanceMode: AppearanceMode,
    hapticsEnabled: Boolean,
    soundsEnabled: Boolean,
    versionName: String,
    onInputMethodSelected: (KeyboardInputMethod) -> Unit,
    onKeySizeSelected: (KeyboardSizingProfile) -> Unit,
    onKeyboardThemeSelected: (KeyboardThemeId) -> Unit,
    onAppearanceSelected: (AppearanceMode) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onSoundsChanged: (Boolean) -> Unit,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit,
    onOpenWebsite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var picker by rememberSaveable { mutableStateOf<SettingsPicker?>(null) }
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            item(key = "title") {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
            item(key = "hero") { SettingsHero() }
            item(key = "keyboard") {
                KeyboardSettingsSection(
                    setupStatus = keyboardSetupStatus,
                    inputMethod = inputMethod,
                    keySizeProfile = keySizeProfile,
                    keyboardThemeId = keyboardThemeId,
                    onOpenPicker = { picker = it },
                    onEnableKeyboard = onEnableKeyboard,
                    onSelectKeyboard = onSelectKeyboard,
                )
            }
            item(key = "appearance") {
                AppearanceSettingsSection(
                    appearanceMode = appearanceMode,
                    onOpenPicker = { picker = it },
                )
            }
            item(key = "feedback") {
                FeedbackSettingsSection(
                    hapticsEnabled = hapticsEnabled,
                    soundsEnabled = soundsEnabled,
                    onHapticsChanged = onHapticsChanged,
                    onSoundsChanged = onSoundsChanged,
                )
            }
            item(key = "about") {
                AboutSettingsSection(
                    versionName = versionName,
                    onOpenWebsite = onOpenWebsite,
                )
            }
        }
    }
    SettingsPickerSheet(
        picker = picker,
        inputMethod = inputMethod,
        keySizeProfile = keySizeProfile,
        keyboardThemeId = keyboardThemeId,
        appearanceMode = appearanceMode,
        onInputMethodSelected = onInputMethodSelected,
        onKeySizeSelected = onKeySizeSelected,
        onKeyboardThemeSelected = onKeyboardThemeSelected,
        onAppearanceSelected = onAppearanceSelected,
        onDismiss = { picker = null },
    )
}
