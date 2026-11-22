package app.funput.funput.ime.clipboard.policy

import app.funput.funput.ime.clipboard.platform.ClipboardContentKind
import app.funput.funput.ime.clipboard.platform.ClipboardReadResult

internal enum class ClipboardOfferKind { TEXT, LINK, SENSITIVE }

/**
 * @property preview One shortened line of the clip, present only when the history already holds
 * its text. Building an offer never reads the clipboard, and a sensitive clip never has one.
 */
internal data class ClipboardOffer(
    val kind: ClipboardOfferKind,
    val sourceToken: String?,
    val contentKind: ClipboardContentKind = ClipboardContentKind.TEXT,
    val preview: String? = null,
)

internal fun ClipboardOffer.matches(read: ClipboardReadResult.Success): Boolean =
    contentKind == read.kind &&
        (kind == ClipboardOfferKind.SENSITIVE) == read.isSensitive &&
        (sourceToken == null || sourceToken == read.sourceToken)
