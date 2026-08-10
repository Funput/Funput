package app.funput.funput.ime.clipboard.platform

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context

internal fun ClipData.readClipboardText(
    context: Context,
    maxLength: Int,
): ClipboardReadResult {
    val snapshot = description.clipboardSnapshot()
    val kind = snapshot.kind ?: return ClipboardReadResult.Unsupported
    repeat(itemCount) { index ->
        val text = getItemAt(index).coerceToText(context)?.toString().orEmpty()
        if (text.isEmpty()) return@repeat
        if (text.length > maxLength) return ClipboardReadResult.TooLarge
        return ClipboardReadResult.Success(
            text = text,
            kind = kind,
            isSensitive = snapshot.isSensitive,
            sourceToken = snapshot.sourceToken ?: ClipboardSourceToken.fromText(text),
        )
    }
    return ClipboardReadResult.Empty
}

internal fun ClipDescription.clipboardSnapshot() = ClipboardSnapshot(
    kind = when {
        hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST) -> ClipboardContentKind.LINK
        hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
            hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) -> ClipboardContentKind.TEXT
        else -> null
    },
    isSensitive = extras?.getBoolean(SensitiveClipboardKey, false) == true,
    sourceToken = ClipboardSourceToken.fromTimestamp(timestamp),
)

internal const val SensitiveClipboardKey = "android.content.extra.IS_SENSITIVE"
