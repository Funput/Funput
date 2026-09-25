package app.funput.funput.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.funput.funput.ui.theme.EntryTracker
import app.funput.funput.ui.theme.staggeredEntry

/** One settings-list entry, keyed for stable scrolling and staggered in by [index] on first show. */
internal fun LazyListScope.settingsItem(
    key: String,
    index: Int,
    tracker: EntryTracker,
    content: @Composable () -> Unit,
) {
    item(key = key) {
        Box(modifier = Modifier.staggeredEntry(index, tracker)) { content() }
    }
}
