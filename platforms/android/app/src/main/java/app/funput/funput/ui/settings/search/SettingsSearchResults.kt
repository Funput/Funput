package app.funput.funput.ui.settings.search

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.FunputRow
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.LinkRow
import app.funput.funput.ui.kit.theme.FunputTint
import app.funput.funput.ui.settings.pages.SettingsPage

/**
 * What the hub shows while searching: one card of matches, each naming the page it opens, or a
 * card saying nothing matched with words to try. Always one item, so the list does not reshuffle.
 */
internal fun LazyListScope.settingsSearchResults(
    results: List<SettingsSearchResult>,
    onOpenPage: (SettingsPage) -> Unit,
) = item(key = "results") {
    FunputSection(title = null, modifier = Modifier.testTag(SettingsSearchResultsTag)) {
        if (results.isEmpty()) {
            FunputRow(
                title = stringResource(R.string.settings_search_empty_title),
                summary = stringResource(R.string.settings_search_empty_hint),
                icon = FunputIcons.Search,
                tint = FunputTint.GRAY,
            )
        }
        results.forEachIndexed { index, result ->
            if (index > 0) FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
            val page = result.entry.page
            LinkRow(
                title = result.title,
                onClick = { onOpenPage(page) },
                summary = stringResource(page.title),
                icon = result.entry.icon,
                tint = page.tint,
            )
        }
    }
}

/** Test tag of the search results card. */
internal const val SettingsSearchResultsTag = "settings-search-results"
