package app.funput.funput.ime.clipboard.policy

/** Shortens clip text to the single line a paste offer shows on the suggestion band. */
internal object ClipboardOfferPreview {
    /** Longest preview, in code points; the band ellipsizes further to fit its width. */
    const val MaxCodePoints = 80

    /** A clip can be 100,000 characters long, so only its opening is ever examined. */
    private const val ScanLength = 400
    private val Whitespace = Regex("\\s+")

    /** Returns null when [text] has nothing visible to show. */
    fun of(text: String): String? {
        val start = text.indexOfFirst { !it.isWhitespace() }
        if (start < 0) return null
        val scanned = text.substring(start, minOf(text.length, start + ScanLength))
        val line = scanned.replace(Whitespace, " ").trimEnd()
        val count = line.codePointCount(0, line.length)
        val cut = count > MaxCodePoints || start + ScanLength < text.length
        if (!cut) return line
        val kept = line.substring(0, line.offsetByCodePoints(0, minOf(count, MaxCodePoints)))
        // The scan window can end between the two halves of a surrogate pair.
        return kept.trimEnd { it.isWhitespace() || it.isHighSurrogate() } + "…"
    }
}
