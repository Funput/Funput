package app.funput.funput.ui.shortcuts

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.shortcuts.model.TextShortcut
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.controls.FunputSearchField
import app.funput.funput.ui.kit.gestures.SwipeToDelete
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.LinkRow
import app.funput.funput.ui.kit.rows.ToggleRow
import app.funput.funput.ui.shortcuts.components.ShortcutsEmptyState
import app.funput.funput.ui.shortcuts.components.ShortcutsLoadStatus

/**
 * The shortcuts page: the feature switch, the search, then the list under a count. In the list's
 * place: a loading or load-error card until the library is read, or an empty state that says
 * whether there is nothing yet or nothing matching.
 */
internal fun LazyListScope.shortcutsSections(
    model: ShortcutsScreenModel,
    onEdit: (TextShortcut) -> Unit,
    onAdd: () -> Unit,
    onDelete: (TextShortcut) -> Unit,
) {
    item(key = "enabled") {
        FunputSection(title = null, footer = stringResource(R.string.shortcuts_reopen_hint)) {
            ToggleRow(
                title = stringResource(R.string.shortcuts_enabled),
                summary = stringResource(R.string.shortcuts_enabled_summary),
                checked = model.library.isEnabled,
                onCheckedChange = { value -> model.updateOptions { it.copy(isEnabled = value) } },
                icon = FunputIcons.Shortcuts,
                enabled = model.canWrite,
            )
        }
    }
    item(key = "search") {
        FunputSearchField(
            query = model.query,
            onQueryChange = { model.query = it },
            placeholder = stringResource(R.string.shortcuts_search),
            clearLabel = stringResource(R.string.shortcuts_clear_search),
            modifier = Modifier.testTag(ShortcutsSearchTag),
        )
    }
    // Always the same three items: loading and load errors show where the list will be, so the list
    // never gains or loses an item as loading finishes (a vanishing item left a gap for a frame).
    item(key = "list") {
        val entries = model.filteredEntries
        val total = model.library.entries.size
        when {
            model.loadError != null -> ShortcutsLoadStatus(loading = false, error = true, retry = model::reload)
            !model.hasLoaded -> ShortcutsLoadStatus(loading = true, error = false, retry = model::reload)
            total == 0 -> ShortcutsEmptyState(
                title = stringResource(R.string.shortcuts_empty_title),
                body = stringResource(R.string.shortcuts_empty_body),
                action = onAdd,
            )
            entries.isEmpty() -> ShortcutsEmptyState(
                title = stringResource(R.string.shortcuts_no_result_title),
                body = stringResource(R.string.shortcuts_no_result_body),
            )
            else -> FunputSection(
                title = countLabel(model.query, entries.size, total),
                footer = if (model.canWrite) stringResource(R.string.shortcuts_swipe_delete) else null,
            ) {
                entries.forEachIndexed { index, entry ->
                    if (index > 0) FunputDivider()
                    SwipeToDelete(
                        label = stringResource(R.string.shortcuts_delete),
                        onDelete = { onDelete(entry) },
                        enabled = model.canWrite,
                        modifier = Modifier.testTag("shortcuts-entry-${entry.id}"),
                    ) {
                        LinkRow(
                            title = entry.trigger,
                            summary = entry.expansion,
                            summaryMaxLines = 2,
                            onClick = { onEdit(entry) },
                            enabled = model.canWrite,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun countLabel(query: String, shown: Int, total: Int): String =
    if (query.isBlank()) stringResource(R.string.shortcuts_list_count, total)
    else stringResource(R.string.shortcuts_result_count, shown, total)

/** Test tag of the search field. */
internal const val ShortcutsSearchTag = "shortcuts-search"
