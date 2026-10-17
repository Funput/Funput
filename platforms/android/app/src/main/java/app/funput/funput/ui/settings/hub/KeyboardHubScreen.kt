package app.funput.funput.ui.settings.hub

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.layout.FunputScreen
import app.funput.funput.ui.settings.SettingsScreenState
import app.funput.funput.ui.settings.pages.SettingsPage
import app.funput.funput.ui.settings.search.rememberSettingsSearchIndex
import app.funput.funput.ui.settings.search.settingsSearchField
import app.funput.funput.ui.settings.search.settingsSearchResults
import app.funput.funput.ui.settings.summary.rememberSettingsSummaries

/**
 * The "Bàn phím" tab: a search over every keyboard setting, then a table of contents for them,
 * each page one row with a sentence saying what it is set to. While the search holds a query, its
 * results take the place of everything under it. The pages open on top, inside this tab.
 */
@Composable
internal fun KeyboardHubScreen(
    state: SettingsScreenState,
    onOpenPage: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
    tabBar: (@Composable () -> Unit)? = null,
) {
    // Saveable, so a page opened from the results comes back to the same results.
    var query by rememberSaveable { mutableStateOf("") }
    val searching = query.isNotBlank()
    // The hub is the root of the start tab, where back would otherwise leave the app.
    BackHandler(enabled = searching) { query = "" }
    val summaries = rememberSettingsSummaries()
    val index = rememberSettingsSearchIndex(state.speechAvailable)
    // Built here, above the list, so no lazy item reads settings that may still be loading.
    val rows = SettingsPage.hubRows(state.speechAvailable).map { page ->
        SettingsHubRow(page, summaries.of(page, state))
    }
    val results = if (searching) index.search(query) else emptyList()
    FunputScreen(title = stringResource(R.string.nav_keyboard), modifier = modifier, tabBar = tabBar) {
        settingsSearchField(query = query, onQueryChange = { query = it })
        if (searching) {
            settingsSearchResults(results = results, onOpenPage = onOpenPage)
        } else {
            keyboardHubItems(state = state, rows = rows, onOpenPage = onOpenPage)
        }
    }
}
