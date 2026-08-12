package app.funput.funput.ime.clipboard.controller

import app.funput.funput.ime.clipboard.model.ClipboardEntry

internal data class ClipboardHistoryState(
    val available: Boolean = false,
    val loading: Boolean = false,
    val entries: List<ClipboardEntry> = emptyList(),
)
