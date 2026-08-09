package app.funput.funput.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.settings.feedback.FeedbackSettingsSection
import app.funput.funput.ui.settings.keyboard.KeyboardSettingsSection
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.settings.components.KeyboardHero
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
import app.funput.funput.ui.theme.Spacing
import app.funput.funput.ui.theme.rememberEntryTracker
import app.funput.funput.ui.theme.staggeredEntry
import app.funput.funput.ui.settings.smart.PersonalSuggestionSettingsSection
import app.funput.funput.ui.settings.smart.SmartSettingsSection

@Composable
internal fun SettingsScreenSections(
    keyboardSetupStatus: KeyboardSetupStatus,
    keyboardTheme: KeyboardThemeDescriptor,
    inputMethod: KeyboardInputMethod,
    showsNumberRow: Boolean,
    toneStyle: ToneStyle,
    keySizeProfile: KeyboardSizingProfile,
    hapticsEnabled: Boolean,
    soundsEnabled: Boolean,
    smartRestoreEnabled: Boolean,
    spellCheckEnabled: Boolean,
    personalSuggestionsEnabled: Boolean,
    contentPadding: PaddingValues,
    onOpenPicker: (SettingsPicker) -> Unit,
    onShowsNumberRowChanged: (Boolean) -> Unit,
    onOpenAppearance: () -> Unit,
    onToneStyleSelected: (ToneStyle) -> Unit,
    onKeySizeSelected: (KeyboardSizingProfile) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onSoundsChanged: (Boolean) -> Unit,
    onSmartRestoreChanged: (Boolean) -> Unit,
    onSpellCheckChanged: (Boolean) -> Unit,
    onPersonalSuggestionsChanged: (Boolean) -> Unit,
    onResetPersonalSuggestions: () -> Unit,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tracker = rememberEntryTracker()
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.Section),
        // The scaffold's padding clears the app bar and the navigation bar; the list itself keeps
        // scrolling underneath both, which is what edge-to-edge is for. Only the horizontal
        // insets are hard padding, so a landscape cutout never clips a row.
        contentPadding = PaddingValues(
            start = Spacing.Large,
            end = Spacing.Large,
            top = contentPadding.calculateTopPadding() + Spacing.Tight,
            bottom = contentPadding.calculateBottomPadding() + Spacing.Section,
        ),
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        item(key = "hero") {
            Box(modifier = Modifier.staggeredEntry(0, tracker)) {
                KeyboardHero(keyboardTheme, onOpenAppearance)
            }
        }
        item(key = "keyboard") {
            Box(modifier = Modifier.staggeredEntry(4, tracker)) {
                KeyboardSettingsSection(
                    setupStatus = keyboardSetupStatus,
                    inputMethod = inputMethod,
                    showsNumberRow = showsNumberRow,
                    toneStyle = toneStyle,
                    keySizeProfile = keySizeProfile,
                    onOpenPicker = onOpenPicker,
                    onShowsNumberRowChanged = onShowsNumberRowChanged,
                    onToneStyleSelected = onToneStyleSelected,
                    onKeySizeSelected = onKeySizeSelected,
                    onEnableKeyboard = onEnableKeyboard,
                    onSelectKeyboard = onSelectKeyboard,
                )
            }
        }
        item(key = "smart") {
            Box(modifier = Modifier.staggeredEntry(2, tracker)) {
                SmartSettingsSection(
                    smartRestoreEnabled = smartRestoreEnabled,
                    spellCheckEnabled = spellCheckEnabled,
                    onSmartRestoreChanged = onSmartRestoreChanged,
                    onSpellCheckChanged = onSpellCheckChanged,
                )
            }
        }
        item(key = "personal-suggestions") {
            Box(modifier = Modifier.staggeredEntry(3, tracker)) {
                PersonalSuggestionSettingsSection(
                    enabled = personalSuggestionsEnabled,
                    onEnabledChanged = onPersonalSuggestionsChanged,
                    onReset = onResetPersonalSuggestions,
                )
            }
        }
        item(key = "feedback") {
            Box(modifier = Modifier.staggeredEntry(4, tracker)) {
                FeedbackSettingsSection(
                    hapticsEnabled = hapticsEnabled,
                    soundsEnabled = soundsEnabled,
                    onHapticsChanged = onHapticsChanged,
                    onSoundsChanged = onSoundsChanged,
                )
            }
        }
    }
}
