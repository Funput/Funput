package app.funput.funput.ime.nativebridge.correction

/**
 * The engine's correction counters, for the end-of-session log. Counts only, never words.
 * Field order is the order `nativeCorrectionMetrics` returns them in.
 */
internal data class CorrectionCounts(
    val applied: Int = 0,
    val reverted: Int = 0,
    val skippedAmbiguous: Int = 0,
    val candidatesMax: Int = 0,
    val microsecondsMax: Int = 0,
    val keptAsTyped: Int = 0,
    val offered: Int = 0,
    val noCandidate: Int = 0,
    val suppressed: Int = 0,
    val validNearEdge: Int = 0,
) {
    companion object {
        private const val Size = 10

        fun decode(encoded: IntArray?): CorrectionCounts {
            if (encoded == null || encoded.size < Size) return CorrectionCounts()
            return CorrectionCounts(
                applied = encoded[0], reverted = encoded[1], skippedAmbiguous = encoded[2],
                candidatesMax = encoded[3], microsecondsMax = encoded[4], keptAsTyped = encoded[5],
                offered = encoded[6], noCandidate = encoded[7], suppressed = encoded[8],
                validNearEdge = encoded[9],
            )
        }
    }
}
