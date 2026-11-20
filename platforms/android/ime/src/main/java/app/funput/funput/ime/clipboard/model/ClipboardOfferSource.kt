package app.funput.funput.ime.clipboard.model

/**
 * What the history knows about the clip currently on the system clipboard.
 *
 * @property lastPastedSourceToken Clipboard generation last pasted through Funput.
 * @property heldText Text of that clip when the history already holds it, so a paste offer can
 * preview it without reading the clipboard.
 */
internal data class ClipboardOfferSource(
    val lastPastedSourceToken: String?,
    val heldText: String?,
)
