package app.funput.funput.ime.nativebridge.correction

/** How sure the engine is of its best candidate, numbered as the C header numbers it. */
internal enum class CorrectionVerdict {
    /** Clear enough to apply without asking. */
    APPLY,

    /** The top two are too close: offer both. */
    AMBIGUOUS,

    /** The word as typed beats every candidate. */
    AS_TYPED,

    /** Nothing the host allowed is left to offer. */
    REFUSED,
}

/** The best and runner-up candidate indexes (null for an empty slot), and the verdict. */
internal data class CorrectionRanking(val best: Int?, val second: Int?, val verdict: CorrectionVerdict) {
    companion object {
        val Refused = CorrectionRanking(null, null, CorrectionVerdict.REFUSED)

        /** Reads `[best, second, verdict]`, where -1 marks an empty slot (never 0). */
        fun decode(encoded: IntArray?): CorrectionRanking {
            if (encoded == null || encoded.size < 3) return Refused
            val verdict = CorrectionVerdict.entries.getOrNull(encoded[2]) ?: return Refused
            return CorrectionRanking(encoded[0].takeIf { it >= 0 }, encoded[1].takeIf { it >= 0 }, verdict)
        }
    }
}
