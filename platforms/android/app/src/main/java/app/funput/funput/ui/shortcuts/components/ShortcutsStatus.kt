package app.funput.funput.ui.shortcuts.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputMessageCard

/** While the library loads, says so; if it failed, says that and offers to try again. */
@Composable
internal fun ShortcutsLoadStatus(loading: Boolean, error: Boolean, retry: () -> Unit) {
    if (!loading && !error) return
    FunputMessageCard(
        title = stringResource(if (loading) R.string.shortcuts_loading else R.string.shortcuts_load_error),
        actionLabel = if (error) stringResource(R.string.shortcuts_retry) else null,
        onAction = retry,
    )
}

/**
 * Nothing to list: [title] and [body] say why (no shortcuts yet, or none matching the search),
 * and [action], when given, offers to add the first one.
 */
@Composable
internal fun ShortcutsEmptyState(title: String, body: String, action: (() -> Unit)? = null) {
    FunputMessageCard(
        title = title,
        body = body,
        actionLabel = action?.let { stringResource(R.string.shortcuts_add) },
        onAction = action ?: {},
    )
}
