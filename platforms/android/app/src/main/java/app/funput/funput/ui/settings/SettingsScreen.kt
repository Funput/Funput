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
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.theme.BrandBlue
import app.funput.funput.ui.theme.BrandOrange
import app.funput.funput.ui.theme.BrandPink
import app.funput.funput.ui.theme.BrandPurple

private enum class SettingsPicker { INPUT_METHOD, APPEARANCE }

@Composable
internal fun SettingsScreen(
    inputMethod: KeyboardInputMethod,
    appearanceMode: AppearanceMode,
    hapticsEnabled: Boolean,
    soundsEnabled: Boolean,
    versionName: String,
    onInputMethodSelected: (KeyboardInputMethod) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
            item { SettingsHero() }
            item {
                SettingsGroup {
                    SettingsRow(
                        title = stringResource(R.string.settings_input_method_title),
                        value = inputMethod.label(),
                        iconRes = R.drawable.ic_keyboard,
                        iconBackground = BrandPurple,
                        onClick = { picker = SettingsPicker.INPUT_METHOD },
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = stringResource(R.string.settings_theme_title),
                        value = appearanceMode.label(),
                        iconRes = R.drawable.ic_appearance,
                        iconBackground = BrandPink,
                        onClick = { picker = SettingsPicker.APPEARANCE },
                    )
                }
            }
            item {
                SettingsGroup {
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
            item {
                SettingsGroup {
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
            item {
                SettingsGroup {
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
