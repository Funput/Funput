package app.funput.funput.ui.settings.dictionary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.funputCardItems
import app.funput.funput.ui.kit.controls.FunputSearchField
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.settings.dictionary.components.DictionaryCountRow
import app.funput.funput.ui.settings.dictionary.components.DictionaryHeaderCard
import app.funput.funput.ui.settings.dictionary.components.DictionaryNotices
import app.funput.funput.ui.settings.dictionary.components.DictionaryWordActions
import app.funput.funput.ui.settings.dictionary.components.DictionaryWordRow
import app.funput.funput.ui.settings.dictionary.model.DictionaryScreenPage
import app.funput.funput.ui.settings.dictionary.model.PersonalDictionaryScreenModel

/**
 * The page top to bottom: what needs fixing, the totals and the list switch, the search, then
 * the words under their count and sort. A store holds up to thousands of words, so each word
 * is its own lazy item; the screen therefore has no item spacing and each section brings its own.
 */
internal fun LazyListScope.personalDictionarySections(
    model: PersonalDictionaryScreenModel,
    page: DictionaryScreenPage,
    settings: DictionarySettings,
    actions: DictionaryWordActions,
    onAdd: () -> Unit,
) {
    if (!settings.suggestionsEnabled || settings.resetPending) {
        section("notices") {
            DictionaryNotices(settings.suggestionsEnabled, settings.resetPending, settings.onEnableSuggestions)
        }
    }
    section("header") { DictionaryHeaderCard(page, onSelect = model::select) }
    section("search") {
        FunputSearchField(
            query = page.search,
            onQueryChange = model::search,
            placeholder = stringResource(R.string.dictionary_search),
            clearLabel = stringResource(R.string.dictionary_clear_search),
            modifier = Modifier.testTag(DictionarySearchTag),
        )
    }
    if (!page.hasLoaded || page.loadFailed || page.words.isEmpty()) {
        section("status") {
            DictionaryListStatus(page, retry = model::reload, clearSearch = { model.search("") }, add = onAdd)
        }
        return
    }
    section("count", headsList = true) { DictionaryCountRow(page, onSort = model::sortBy) }
    funputCardItems(page.words, key = { it.text }) { word ->
        DictionaryWordRow(word, page.highestUses, actions)
    }
}

/** One section of the page with the section gap above it, and a small one below if it [headsList]. */
private fun LazyListScope.section(key: String, headsList: Boolean = false, content: @Composable () -> Unit) {
    item(key = key) {
        val spacing = FunputUi.spacing
        Box(Modifier.padding(top = spacing.section, bottom = if (headsList) spacing.small else 0.dp)) { content() }
    }
}

/** Test tag of the search field. */
internal const val DictionarySearchTag = "dictionary-search"
