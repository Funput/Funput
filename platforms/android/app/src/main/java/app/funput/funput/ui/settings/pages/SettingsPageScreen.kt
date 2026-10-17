package app.funput.funput.ui.settings.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.ui.kit.layout.FunputScreen
import app.funput.funput.ui.settings.LocalSettingsPageTitle
import app.funput.funput.ui.settings.SettingsPicker
import app.funput.funput.ui.settings.SettingsPickerSheet
import app.funput.funput.ui.settings.SettingsScreenState

/**
 * The frame every settings page shares: its title in the top bar, back to wherever it was opened
 * from, and its own picker sheet. A section titled like the page drops its header, so a page of
 * one section does not name itself twice.
 */
@Composable
internal fun SettingsPageScreen(
    page: SettingsPage,
    state: SettingsScreenState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Per destination, so the navigation host forgets it once the page is popped.
    var picker by rememberSaveable { mutableStateOf<SettingsPicker?>(null) }
    val title = stringResource(page.title)
    CompositionLocalProvider(LocalSettingsPageTitle provides title) {
        FunputScreen(title = title, modifier = modifier, onBack = onBack) {
            settingsPageItems(page = page, state = state, onOpenPicker = { picker = it })
        }
    }
    SettingsPickerSheet(picker = picker, state = state, onDismiss = { picker = null })
}
