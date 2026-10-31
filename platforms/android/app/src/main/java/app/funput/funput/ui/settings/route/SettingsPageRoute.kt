package app.funput.funput.ui.settings.route

import androidx.compose.runtime.Composable
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.FunputSettingsState
import app.funput.funput.ui.settings.pages.SettingsPage
import app.funput.funput.ui.settings.pages.SettingsPageScreen

/** One settings page bound to the persisted settings, the same ones the hub reads. */
@Composable
internal fun SettingsPageRoute(
    page: SettingsPage,
    settings: FunputSettingsState,
    keyboardTheme: KeyboardThemeDescriptor,
    onBack: () -> Unit,
    onOpenDictionary: () -> Unit = {},
) {
    SettingsPageScreen(
        page = page,
        state = rememberSettingsScreenState(settings, keyboardTheme, onOpenAppearance = {}),
        onBack = onBack,
        onOpenDictionary = onOpenDictionary,
    )
}
