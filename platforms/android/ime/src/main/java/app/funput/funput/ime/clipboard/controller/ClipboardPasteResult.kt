package app.funput.funput.ime.clipboard.controller

import app.funput.funput.ime.clipboard.platform.ClipboardReadResult

internal enum class ClipboardPasteResult {
    PASTED,
    BLOCKED,
    BUSY,
    EMPTY,
    UNSUPPORTED,
    TOO_LARGE,
    UNAVAILABLE,
    CHANGED,
}

internal fun ClipboardReadResult.toPasteResult() = when (this) {
    ClipboardReadResult.Empty -> ClipboardPasteResult.EMPTY
    ClipboardReadResult.Unsupported -> ClipboardPasteResult.UNSUPPORTED
    ClipboardReadResult.TooLarge -> ClipboardPasteResult.TOO_LARGE
    ClipboardReadResult.Unavailable -> ClipboardPasteResult.UNAVAILABLE
    is ClipboardReadResult.Success -> ClipboardPasteResult.PASTED
}
