package app.funput.funput.ime.clipboard.platform

internal enum class ClipboardContentKind { TEXT, LINK }

internal data class ClipboardSnapshot(
    val kind: ClipboardContentKind?,
    val isSensitive: Boolean,
    val sourceToken: String?,
)

internal sealed interface ClipboardReadResult {
    data class Success(
        val text: String,
        val kind: ClipboardContentKind,
        val isSensitive: Boolean,
        val sourceToken: String,
    ) : ClipboardReadResult

    data object Empty : ClipboardReadResult
    data object Unsupported : ClipboardReadResult
    data object TooLarge : ClipboardReadResult
    data object Unavailable : ClipboardReadResult
}

internal fun interface ClipboardObservation {
    fun close()
}

internal interface ClipboardGateway {
    fun snapshot(): ClipboardSnapshot?
    fun readText(maxLength: Int): ClipboardReadResult
    fun observe(onChanged: () -> Unit): ClipboardObservation
}
