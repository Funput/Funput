package app.funput.funput.ui.settings.search

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import app.funput.funput.ui.settings.pages.SettingsPage

/** A setting the query found, with its title already in words. */
internal class SettingsSearchResult(val entry: SettingsSearchEntry, val title: String)

/**
 * The settings search over [entries], resolved against [resources] once: each entry's title,
 * summary and keywords folded into one string the matcher looks through.
 */
internal class SettingsSearchIndex(
    resources: Resources,
    speechAvailable: Boolean,
    entries: List<SettingsSearchEntry> = SettingsSearchEntries,
) {
    private val indexed = entries
        .filter { entry -> speechAvailable || entry.page != SettingsPage.SPEECH }
        .map { entry ->
            val title = resources.getString(entry.title)
            val text = listOfNotNull(title, entry.summary?.let(resources::getString)) + entry.keywords
            SettingsSearchResult(entry, title) to SettingsSearchMatcher.normalize(text.joinToString(" "))
        }

    /** Every page an entry can open, so a test can check none is unreachable by search. */
    val pages: Set<SettingsPage> get() = indexed.map { (result, _) -> result.entry.page }.toSet()

    /** The settings matching [query], in index order; nothing for a blank query. */
    fun search(query: String): List<SettingsSearchResult> {
        val words = SettingsSearchMatcher.words(query)
        return indexed.filter { (_, haystack) -> SettingsSearchMatcher.matches(words, haystack) }.map { it.first }
    }
}

/** The search index for the current resources. */
@Composable
internal fun rememberSettingsSearchIndex(speechAvailable: Boolean): SettingsSearchIndex {
    val resources = LocalResources.current
    return remember(resources, speechAvailable) { SettingsSearchIndex(resources, speechAvailable) }
}
