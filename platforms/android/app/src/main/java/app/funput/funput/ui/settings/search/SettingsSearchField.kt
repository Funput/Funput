package app.funput.funput.ui.settings.search

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.controls.FunputSearchField

/**
 * The settings search, right under the hub's title. Its key never changes, so typing keeps the
 * focus while everything under it swaps between the hub and the results.
 */
internal fun LazyListScope.settingsSearchField(query: String, onQueryChange: (String) -> Unit) = item(key = "search") {
    FunputSearchField(
        query = query,
        onQueryChange = onQueryChange,
        placeholder = stringResource(R.string.settings_search_placeholder),
        clearLabel = stringResource(R.string.settings_search_clear),
        modifier = Modifier.testTag(SettingsSearchFieldTag),
    )
}

/** Test tag of the settings search field. */
internal const val SettingsSearchFieldTag = "settings-search"
