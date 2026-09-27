package app.funput.funput.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.layout.FunputScreen

/**
 * The Settings tab: a FunputUI screen of grouped settings, with the picker sheet the rows open.
 * The app passes its [tabBar], which the screen draws as glass over its own content.
 */
@Composable
internal fun SettingsScreen(
    state: SettingsScreenState,
    modifier: Modifier = Modifier,
    tabBar: (@Composable () -> Unit)? = null,
) {
    var picker by rememberSaveable { mutableStateOf<SettingsPicker?>(null) }
    FunputScreen(title = stringResource(R.string.settings_title), modifier = modifier, tabBar = tabBar) {
        settingsSections(state = state, onOpenPicker = { picker = it })
    }
    SettingsPickerSheet(picker = picker, state = state, onDismiss = { picker = null })
}
