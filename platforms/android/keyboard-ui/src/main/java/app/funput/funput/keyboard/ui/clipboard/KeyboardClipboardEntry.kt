package app.funput.funput.keyboard.ui.clipboard

import java.time.Instant
import java.util.UUID

/** One persisted clipboard row presented by the keyboard UI. */
data class KeyboardClipboardEntry(
    val id: UUID,
    val text: String,
    val capturedAt: Instant,
    val isPinned: Boolean,
) {
    init { require(text.isNotEmpty()) { "Clipboard text must not be empty" } }
}

internal data class ClipboardHistoryGroup(
    val pinned: Boolean,
    val entries: List<KeyboardClipboardEntry>,
)

internal fun clipboardGroups(entries: List<KeyboardClipboardEntry>): List<ClipboardHistoryGroup> {
    val newest = entries.sortedByDescending(KeyboardClipboardEntry::capturedAt)
    return listOf(
        ClipboardHistoryGroup(true, newest.filter(KeyboardClipboardEntry::isPinned)),
        ClipboardHistoryGroup(false, newest.filterNot(KeyboardClipboardEntry::isPinned)),
    ).filter { it.entries.isNotEmpty() }
}
