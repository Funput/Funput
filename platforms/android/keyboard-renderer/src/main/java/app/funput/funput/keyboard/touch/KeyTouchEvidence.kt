package app.funput.funput.keyboard.touch

/**
 * Where a finger landed, for the typo-correction engine: the key it typed and how far from
 * that key's centre it came down, plus up to three other keys it was nearly as close to.
 *
 * Distances are in key pitches (a canonical key plus its gap), measured on each axis
 * separately and then combined, so a slip sideways and a slip onto the next row weigh the
 * same however tall the keys are drawn. Codepoints are the keys' lower-case labels.
 */
data class KeyTouchEvidence(
    val typed: Int,
    val typedDistance: Float,
    val alternates: List<KeyTouchAlternate> = emptyList(),
) {
    init {
        require(alternates.size <= MaxAlternates) { "At most $MaxAlternates alternates" }
    }

    companion object {
        /** How many neighbours the engine reads per key. */
        const val MaxAlternates = 3

        /** Further than this from a key's centre, a finger was not aiming at it. */
        const val PlausibleDistance = 1.2f
    }
}

/** A key the finger was nearly as close to as the one it typed. */
data class KeyTouchAlternate(val codePoint: Int, val distance: Float)
