package app.funput.funput.ime.editing.caret

/**
 * Line structure around the caret, derived from the text the host hands back.
 *
 * Lines here are logical — newline-separated — not the soft-wrapped lines the user sees. An IME
 * has no access to the host's text layout, so wrap positions are unknowable from this side.
 *
 * With an `ExtractedText` the window is the whole field and the arithmetic is exact. On the
 * windowed fallback a truncated window can only shorten a jump to the line's edge: the caret still
 * lands inside the real line, never outside the document.
 */
internal class CaretLineGeometry(context: KeyboardCaretContext) {
    /**
     * @property offset characters to move the caret by, relative to where it is now.
     * @property column the column the pan is aiming for, carried into the next vertical step so
     *   passing through a short line does not permanently pull the caret left. Null when the move
     *   was purely horizontal and no column was ever computed.
     */
    data class Resolution(val offset: Int, val column: Int?)

    /** Characters between the start of the caret's line and the caret. */
    val column: Int

    /** Characters between the caret and the end of its line. */
    private val trailing: Int

    /** Length of every line in the window, in order. */
    private val lineLengths: List<Int>

    /** Index of the caret's own line within [lineLengths]. */
    private val lineIndex: Int

    init {
        // Split on '\n' alone rather than on every newline kind: every separator is then exactly
        // one character, which is what keeps the offset arithmetic exact. A stray '\r' counts as
        // an ordinary column character, which is harmless and still lands the caret correctly.
        val before = context.before.split('\n')
        val after = context.after.split('\n')
        column = before.last().length
        trailing = after.first().length
        lineIndex = before.size - 1
        lineLengths = before.dropLast(1).map { it.length } +
            listOf(column + trailing) +
            after.drop(1).map { it.length }
    }

    /**
     * Resolves a trackpad step into one caret offset.
     *
     * @param desiredColumn the column an earlier step in the same pan was aiming for, or null to
     *   take the caret's current column as the target.
     */
    fun resolve(columns: Int, lines: Int, desiredColumn: Int?): Resolution {
        // The horizontal-only case needs no line structure at all, and stays exactly the offset
        // the caller asked for.
        if (lines == 0) return Resolution(columns, null)
        val target = (lineIndex + lines).coerceIn(0, lineLengths.lastIndex)
        if (target == lineIndex) {
            // Nowhere left to go on that side. Falling back to the line's own edge is what an
            // up-arrow on the first line does everywhere else in the system, and it is the only
            // thing a vertical drag can usefully mean in a single-line field.
            return if (lines < 0) {
                Resolution(-column, 0)
            } else {
                Resolution(trailing, column + trailing)
            }
        }
        // The horizontal component of a diagonal drag still applies; the remembered column only
        // replaces the one the caret happens to sit on right now.
        val wanted = maxOf(0, (desiredColumn ?: column) + columns)
        val landing = minOf(wanted, lineLengths[target])
        return Resolution(distanceTo(target, landing), wanted)
    }

    private fun distanceTo(target: Int, landing: Int): Int {
        if (target < lineIndex) {
            val skipped = lineLengths.subList(target + 1, lineIndex).sum()
            val newlines = lineIndex - target
            return -(column + skipped + newlines + lineLengths[target] - landing)
        }
        val skipped = lineLengths.subList(lineIndex + 1, target).sum()
        return trailing + skipped + (target - lineIndex) + landing
    }
}
