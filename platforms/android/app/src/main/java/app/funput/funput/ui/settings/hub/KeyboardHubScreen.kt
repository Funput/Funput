package app.funput.funput.ui.settings.hub

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.layout.FunputScreen
import app.funput.funput.ui.settings.SettingsScreenState
import app.funput.funput.ui.settings.pages.SettingsPage
import app.funput.funput.ui.settings.summary.rememberSettingsSummaries

/**
 * The "Bàn phím" tab: a table of contents for the keyboard settings, each page one row with a
 * sentence saying what it is set to. The pages themselves open on top, inside this tab.
 */
@Composable
internal fun KeyboardHubScreen(
    state: SettingsScreenState,
    onOpenPage: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
    tabBar: (@Composable () -> Unit)? = null,
) {
    val summaries = rememberSettingsSummaries()
    // Built here, above the list, so no lazy item reads settings that may still be loading.
    val rows = SettingsPage.hubRows(state.speechAvailable).map { page ->
        SettingsHubRow(page, summaries.of(page, state))
    }
    FunputScreen(title = stringResource(R.string.nav_keyboard), modifier = modifier, tabBar = tabBar) {
        keyboardHubItems(state = state, rows = rows, onOpenPage = onOpenPage)
    }
}
