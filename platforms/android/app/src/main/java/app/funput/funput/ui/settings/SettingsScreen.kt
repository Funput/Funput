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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.theme.BrandBlue
import app.funput.funput.ui.theme.BrandOrange
import app.funput.funput.ui.theme.BrandPink
import app.funput.funput.ui.theme.BrandPurple

private enum class SettingsPicker { INPUT_METHOD, KEY_SIZE, KEYBOARD_THEME, APPEARANCE }

@Composable
internal fun SettingsScreen(
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
    onOpenKeyboardSettings: () -> Unit,
    onShowKeyboardPicker: () -> Unit,
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
                SettingsSection(title = stringResource(R.string.settings_section_keyboard)) {
                    SettingsRow(
                        title = stringResource(R.string.settings_input_method_title),
                        value = inputMethod.label(),
                        iconRes = R.drawable.ic_keyboard,
                        iconBackground = BrandPurple,
                        onClick = { picker = SettingsPicker.INPUT_METHOD },
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = stringResource(R.string.settings_key_size_title),
                        value = keySizeProfile.label(),
                        iconRes = R.drawable.ic_key_size,
                        iconBackground = BrandOrange,
                        onClick = { picker = SettingsPicker.KEY_SIZE },
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = stringResource(R.string.settings_keyboard_theme_title),
                        value = keyboardThemeId.label(),
                        iconRes = R.drawable.ic_keyboard_theme,
                        iconBackground = BrandBlue,
                        onClick = { picker = SettingsPicker.KEYBOARD_THEME },
                    )
                }
            }
            item(key = "appearance") {
                SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                    SettingsRow(
                        title = stringResource(R.string.settings_theme_title),
                        value = appearanceMode.label(),
                        iconRes = R.drawable.ic_appearance,
                        iconBackground = BrandPink,
                        onClick = { picker = SettingsPicker.APPEARANCE },
                    )
                }
            }
            item(key = "feedback") {
                SettingsSection(title = stringResource(R.string.settings_section_feedback)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_haptics_title),
                        checked = hapticsEnabled,
                        iconRes = R.drawable.ic_haptic,
                        iconBackground = BrandBlue,
                        onCheckedChange = onHapticsChanged,
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_sounds_title),
                        checked = soundsEnabled,
                        iconRes = R.drawable.ic_sound,
                        iconBackground = BrandOrange,
                        onCheckedChange = onSoundsChanged,
                    )
                }
            }
            item(key = "system") {
                SettingsSection(title = stringResource(R.string.settings_section_system)) {
                    SettingsRow(
                        title = stringResource(R.string.settings_manage_keyboards_title),
                        iconRes = R.drawable.ic_settings,
                        iconBackground = BrandPurple,
                        onClick = onOpenKeyboardSettings,
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = stringResource(R.string.settings_choose_keyboard_title),
                        iconRes = R.drawable.ic_globe,
                        iconBackground = BrandPink,
                        iconColor = Color(0xFF321700),
                        onClick = onShowKeyboardPicker,
                    )
                }
            }
            item(key = "about") {
                SettingsSection(title = stringResource(R.string.settings_section_about)) {
                    SettingsValueRow(
                        title = stringResource(R.string.settings_version_title),
                        value = versionName,
                        iconRes = R.drawable.ic_info,
                        iconBackground = BrandBlue,
                    )
                }
            }
        }
    }
    when (picker) {
        SettingsPicker.INPUT_METHOD -> PreferencePickerSheet(
            title = stringResource(R.string.settings_input_method_title),
            options = inputMethodOptions(),
            selected = inputMethod,
            onSelected = onInputMethodSelected,
            onDismiss = { picker = null },
        )
        SettingsPicker.KEY_SIZE -> PreferencePickerSheet(
            title = stringResource(R.string.settings_key_size_title),
            options = keySizeOptions(),
            selected = keySizeProfile,
            onSelected = onKeySizeSelected,
            onDismiss = { picker = null },
        )
        SettingsPicker.KEYBOARD_THEME -> PreferencePickerSheet(
            title = stringResource(R.string.settings_keyboard_theme_title),
            options = keyboardThemeOptions(),
            selected = keyboardThemeId,
            onSelected = onKeyboardThemeSelected,
            onDismiss = { picker = null },
        )
        SettingsPicker.APPEARANCE -> PreferencePickerSheet(
            title = stringResource(R.string.settings_theme_title),
            options = appearanceOptions(),
            selected = appearanceMode,
            onSelected = onAppearanceSelected,
            onDismiss = { picker = null },
        )
        null -> Unit
    }
}
