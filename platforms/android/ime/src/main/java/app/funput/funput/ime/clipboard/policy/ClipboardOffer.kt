package app.funput.funput.ime.clipboard.policy

internal enum class ClipboardOfferKind { TEXT, LINK, SENSITIVE }

internal data class ClipboardOffer(
    val kind: ClipboardOfferKind,
    val sourceToken: String?,
)
