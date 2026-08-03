package app.funput.funput.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.settings.about.AboutSettingsSection
import app.funput.funput.ui.settings.appearance.AppearanceSettingsSection
import app.funput.funput.ui.settings.components.SettingsHero
import app.funput.funput.ui.settings.feedback.FeedbackSettingsSection
import app.funput.funput.ui.settings.keyboard.KeyboardSettingsSection
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
import app.funput.funput.ui.settings.smart.PersonalSuggestionSettingsSection
import app.funput.funput.ui.settings.smart.SmartSettingsSection

@Composable
internal fun SettingsScreenSections(
    keyboardSetupStatus: KeyboardSetupStatus,
    inputMethod: KeyboardInputMethod,
    showsNumberRow: Boolean,
    toneStyle: ToneStyle,
    keySizeProfile: KeyboardSizingProfile,
    keyboardThemeLabel: String,
    appearanceMode: AppearanceMode,
    hapticsEnabled: Boolean,
    soundsEnabled: Boolean,
    smartRestoreEnabled: Boolean,
    spellCheckEnabled: Boolean,
    personalSuggestionsEnabled: Boolean,
    versionName: String,
    onOpenPicker: (SettingsPicker) -> Unit,
    onShowsNumberRowChanged: (Boolean) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onSoundsChanged: (Boolean) -> Unit,
    onSmartRestoreChanged: (Boolean) -> Unit,
    onSpellCheckChanged: (Boolean) -> Unit,
    onPersonalSuggestionsChanged: (Boolean) -> Unit,
    onResetPersonalSuggestions: () -> Unit,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit,
    onOpenThemeGallery: () -> Unit,
    onOpenWebsite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        modifier = modifier
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
                showsNumberRow = showsNumberRow,
                toneStyle = toneStyle,
                keySizeProfile = keySizeProfile,
                keyboardThemeLabel = keyboardThemeLabel,
                onOpenPicker = onOpenPicker,
                onShowsNumberRowChanged = onShowsNumberRowChanged,
                onOpenThemeGallery = onOpenThemeGallery,
                onEnableKeyboard = onEnableKeyboard,
                onSelectKeyboard = onSelectKeyboard,
            )
        }
        item(key = "smart") {
            SmartSettingsSection(
                smartRestoreEnabled = smartRestoreEnabled,
                spellCheckEnabled = spellCheckEnabled,
                onSmartRestoreChanged = onSmartRestoreChanged,
                onSpellCheckChanged = onSpellCheckChanged,
            )
        }
        item(key = "personal-suggestions") {
            PersonalSuggestionSettingsSection(
                enabled = personalSuggestionsEnabled,
                onEnabledChanged = onPersonalSuggestionsChanged,
                onReset = onResetPersonalSuggestions,
            )
        }
        item(key = "appearance") {
            AppearanceSettingsSection(
                appearanceMode = appearanceMode,
                onOpenPicker = onOpenPicker,
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
