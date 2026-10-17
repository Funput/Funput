package app.funput.funput.ime.nativebridge.correction

/** Delete [backspace] characters before the caret, then insert [text]. */
internal data class CorrectionEdit(val backspace: Int, val text: String) {
    companion object {
        /** Reads the engine's `[backspace, codepoint…]`; an empty or absent array is a no-op. */
        fun decode(encoded: IntArray?): CorrectionEdit? {
            if (encoded == null || encoded.isEmpty()) return null
            return CorrectionEdit(encoded[0], String(encoded, 1, encoded.size - 1))
        }
    }
}
