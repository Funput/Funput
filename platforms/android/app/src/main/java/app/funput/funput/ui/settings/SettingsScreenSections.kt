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
import app.funput.funput.ime.settings.ClipboardPreferences
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.settings.clipboard.clipboardSettingsItem
import app.funput.funput.ui.settings.components.KeyboardHero
import app.funput.funput.ui.settings.feedback.FeedbackSettingsSection
import app.funput.funput.ui.settings.keyboard.KeyboardSettingsSection
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
import app.funput.funput.ui.settings.smart.PersonalSuggestionSettingsSection
import app.funput.funput.ui.settings.smart.SmartSettingsSection
import app.funput.funput.ui.settings.smart.gestureSettingsItem
import app.funput.funput.ui.theme.Spacing
import app.funput.funput.ui.theme.rememberEntryTracker
import app.funput.funput.ui.theme.staggeredEntry
import app.funput.funput.theme.KeyboardThemeDescriptor

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
    smartComposition: SmartCompositionPreferences,
    personalSuggestionsEnabled: Boolean,
    clipboardPreferences: ClipboardPreferences,
    smartGesturesEnabled: Boolean,
    contentPadding: PaddingValues,
    onOpenPicker: (SettingsPicker) -> Unit,
    onShowsNumberRowChanged: (Boolean) -> Unit,
    onOpenAppearance: () -> Unit,
    onToneStyleSelected: (ToneStyle) -> Unit,
    onKeySizeSelected: (KeyboardSizingProfile) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onSoundsChanged: (Boolean) -> Unit,
    onSmartGesturesChanged: (Boolean) -> Unit,
    onSmartRestoreChanged: (Boolean) -> Unit,
    onSpellCheckChanged: (Boolean) -> Unit,
    onAutoCapitalizeChanged: (Boolean) -> Unit,
    onPersonalSuggestionsChanged: (Boolean) -> Unit,
    onClipboardEnabledChanged: (Boolean) -> Unit,
    onClearClipboardHistory: () -> Unit,
    onResetPersonalSuggestions: () -> Unit,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tracker = rememberEntryTracker()
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.Section),
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
                KeyboardHero(
                    descriptor = keyboardTheme,
                    inputMethod = inputMethod,
                    sizingProfile = keySizeProfile,
                    showsNumberRow = showsNumberRow,
                    onOpenAppearance = onOpenAppearance,
                )
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
                    preferences = smartComposition,
                    onSmartRestoreChanged = onSmartRestoreChanged,
                    onSpellCheckChanged = onSpellCheckChanged,
                    onAutoCapitalizeChanged = onAutoCapitalizeChanged,
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
        clipboardSettingsItem(
            preferences = clipboardPreferences,
            tracker = tracker,
            onEnabledChanged = onClipboardEnabledChanged,
            onOpenExpiry = { onOpenPicker(SettingsPicker.CLIPBOARD_EXPIRY) },
            onClear = onClearClipboardHistory,
        )
        item(key = "feedback") {
            Box(modifier = Modifier.staggeredEntry(5, tracker)) {
                FeedbackSettingsSection(
                    hapticsEnabled = hapticsEnabled,
                    soundsEnabled = soundsEnabled,
                    onHapticsChanged = onHapticsChanged,
                    onSoundsChanged = onSoundsChanged,
                )
            }
        }
        gestureSettingsItem(smartGesturesEnabled, tracker, onSmartGesturesChanged)
    }
}
