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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.funput.funput.ui.settings.clipboard.ClipboardSettingsSection
import app.funput.funput.ui.settings.components.KeyboardHero
import app.funput.funput.ui.settings.data.DataSettingsSection
import app.funput.funput.ui.settings.feedback.FeedbackSettingsSection
import app.funput.funput.ui.settings.keyboard.KeyboardSetupCard
import app.funput.funput.ui.settings.keyboard.LayoutSettingsSection
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
import app.funput.funput.ui.settings.smart.SmartSettingsSection
import app.funput.funput.ui.settings.typing.TypingSettingsSection
import app.funput.funput.ui.theme.EntryTracker
import app.funput.funput.ui.theme.Spacing
import app.funput.funput.ui.theme.rememberEntryTracker
import app.funput.funput.ui.theme.staggeredEntry

@Composable
internal fun SettingsScreenSections(
    state: SettingsScreenState,
    contentPadding: PaddingValues,
    onOpenPicker: (SettingsPicker) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tracker = rememberEntryTracker()
    val firstSectionIndex = if (state.keyboardSetupStatus == KeyboardSetupStatus.READY) 1 else 2
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
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .testTag(SettingsListTag),
    ) {
        settingsItem("hero", 0, tracker) {
            KeyboardHero(
                descriptor = state.keyboardTheme,
                inputMethod = state.inputMethod,
                sizingProfile = state.keySizeProfile,
                placement = state.placement,
                showsNumberRow = state.showsNumberRow,
                onOpenAppearance = state.onOpenAppearance,
                modifier = Modifier.testTag(SettingsHeroTag),
            )
        }
        if (state.keyboardSetupStatus != KeyboardSetupStatus.READY) {
            settingsItem("setup", 1, tracker) {
                KeyboardSetupCard(
                    status = state.keyboardSetupStatus,
                    onEnableKeyboard = state.onEnableKeyboard,
                    onSelectKeyboard = state.onSelectKeyboard,
                    modifier = Modifier.testTag(SettingsSetupTag),
                )
            }
        }
        settingsItem("typing", firstSectionIndex, tracker) {
            TypingSettingsSection(
                inputMethod = state.inputMethod,
                toneStyle = state.toneStyle,
                onOpenPicker = onOpenPicker,
                onToneStyleSelected = state.onToneStyleSelected,
                onOpenShortcuts = state.onOpenShortcuts,
            )
        }
        settingsItem("layout", firstSectionIndex + 1, tracker) {
            LayoutSettingsSection(
                inputMethod = state.inputMethod,
                showsNumberRow = state.showsNumberRow,
                keySizeProfile = state.keySizeProfile,
                placement = state.placement,
                onShowsNumberRowChanged = state.onShowsNumberRowChanged,
                onKeySizeSelected = state.onKeySizeSelected,
                onOpenPlacement = { onOpenPicker(SettingsPicker.KEYBOARD_PLACEMENT) },
                onElevationSelected = state.onElevationSelected,
                onOneHandedWidthSelected = state.onOneHandedWidthSelected,
                onOneHandedSideSelected = state.onOneHandedSideSelected,
            )
        }
        settingsItem("smart", firstSectionIndex + 2, tracker) {
            SmartSettingsSection(
                preferences = state.smartComposition,
                personalSuggestionsEnabled = state.personalSuggestionsEnabled,
                smartGesturesEnabled = state.smartGesturesEnabled,
                onSmartRestoreChanged = state.onSmartRestoreChanged,
                onSpellCheckChanged = state.onSpellCheckChanged,
                onAutoCapitalizeChanged = state.onAutoCapitalizeChanged,
                onPersonalSuggestionsChanged = state.onPersonalSuggestionsChanged,
                onSmartGesturesChanged = state.onSmartGesturesChanged,
            )
        }
        settingsItem("feedback", firstSectionIndex + 3, tracker) {
            FeedbackSettingsSection(
                hapticsEnabled = state.hapticsEnabled,
                soundsEnabled = state.soundsEnabled,
                onHapticsChanged = state.onHapticsChanged,
                onSoundsChanged = state.onSoundsChanged,
            )
        }
        settingsItem("clipboard", firstSectionIndex + 4, tracker) {
            ClipboardSettingsSection(
                enabled = state.clipboardPreferences.enabled,
                expiry = state.clipboardPreferences.expiry,
                onEnabledChanged = state.onClipboardEnabledChanged,
                onOpenExpiry = { onOpenPicker(SettingsPicker.CLIPBOARD_EXPIRY) },
            )
        }
        settingsItem("data", firstSectionIndex + 5, tracker) {
            DataSettingsSection(
                onResetPersonalSuggestions = state.onResetPersonalSuggestions,
                onClearClipboardHistory = state.onClearClipboardHistory,
            )
        }
    }
}

private fun LazyListScope.settingsItem(
    key: String,
    index: Int,
    tracker: EntryTracker,
    content: @Composable () -> Unit,
) {
    item(key = key) {
        Box(modifier = Modifier.staggeredEntry(index, tracker)) { content() }
    }
}

internal const val SettingsListTag = "settings-list"
internal const val SettingsHeroTag = "settings-hero"
internal const val SettingsSetupTag = "settings-setup"
