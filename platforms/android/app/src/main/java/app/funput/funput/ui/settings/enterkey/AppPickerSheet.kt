package app.funput.funput.ui.settings.enterkey

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputCard
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.controls.FunputSearchField
import app.funput.funput.ui.kit.overlays.FunputSheet
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.settings.enterkey.components.AppRow
import app.funput.funput.ui.settings.search.SettingsSearchMatcher

/**
 * Picks one launcher app to add, by name, with a search that ignores Vietnamese accents. [apps]
 * is null while the list is still being read.
 */
@Composable
internal fun AppPickerSheet(apps: List<InstalledApp>?, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    FunputSheet(onDismiss = onDismiss, title = stringResource(R.string.settings_enter_key_picker_title)) {
        FunputSearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.settings_enter_key_picker_search),
            clearLabel = stringResource(R.string.settings_enter_key_picker_clear),
        )
        val matches = remember(apps, query) { apps?.let { filterApps(it, query) } }
        when {
            matches == null -> Message(stringResource(R.string.settings_enter_key_picker_loading))
            matches.isEmpty() -> Message(stringResource(R.string.settings_enter_key_picker_empty))
            // A card that shrinks to its rows when few match, and scrolls lazily when many do, so
            // only the icons of rows on screen are ever decoded.
            else -> FunputCard(Modifier.weight(1f, fill = false)) {
                LazyColumn {
                    itemsIndexed(matches, key = { _, app -> app.packageName }) { index, app ->
                        if (index > 0) FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
                        AppRow(
                            packageName = app.packageName,
                            title = app.label,
                            modifier = Modifier
                                .testTag("enter-key-pick-${app.packageName}")
                                .clickable(role = Role.Button) { onPick(app.packageName) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    BasicText(text, style = FunputUi.typography.body.copy(color = FunputUi.colors.secondaryLabel))
}

/** Apps whose name (or package name) holds every word of [query], accents or not. */
internal fun filterApps(apps: List<InstalledApp>, query: String): List<InstalledApp> {
    val words = SettingsSearchMatcher.words(query)
    if (words.isEmpty()) return apps
    return apps.filter { app ->
        SettingsSearchMatcher.matches(words, SettingsSearchMatcher.normalize("${app.label} ${app.packageName}"))
    }
}
