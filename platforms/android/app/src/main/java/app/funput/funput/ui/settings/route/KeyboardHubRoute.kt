package app.funput.funput.ui.settings.route

import androidx.compose.runtime.Composable
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.FunputSettingsState
import app.funput.funput.ui.settings.hub.KeyboardHubScreen
import app.funput.funput.ui.settings.pages.SettingsPage

/** The "Bàn phím" tab bound to the persisted settings. */
@Composable
internal fun KeyboardHubRoute(
    settings: FunputSettingsState,
    keyboardTheme: KeyboardThemeDescriptor,
    onOpenAppearance: () -> Unit,
    onOpenPage: (SettingsPage) -> Unit,
    tabBar: @Composable () -> Unit,
) {
    KeyboardHubScreen(
        state = rememberSettingsScreenState(settings, keyboardTheme, onOpenAppearance),
        onOpenPage = onOpenPage,
        tabBar = tabBar,
    )
}
