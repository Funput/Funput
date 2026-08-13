package app.funput.funput.ime.clipboard.policy

import app.funput.funput.ime.clipboard.platform.ClipboardContentKind
import app.funput.funput.ime.clipboard.platform.ClipboardReadResult

internal enum class ClipboardOfferKind { TEXT, LINK, SENSITIVE }

internal data class ClipboardOffer(
    val kind: ClipboardOfferKind,
    val sourceToken: String?,
    val contentKind: ClipboardContentKind = ClipboardContentKind.TEXT,
)

internal fun ClipboardOffer.matches(read: ClipboardReadResult.Success): Boolean =
    contentKind == read.kind &&
        (kind == ClipboardOfferKind.SENSITIVE) == read.isSensitive &&
        (sourceToken == null || sourceToken == read.sourceToken)
