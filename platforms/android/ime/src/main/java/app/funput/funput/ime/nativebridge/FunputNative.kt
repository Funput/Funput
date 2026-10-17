package app.funput.funput.ime.nativebridge

/** Narrow JNI surface. All calls are synchronous on the IME main thread. */
internal object FunputNative {
    init {
        System.loadLibrary("funput_jni")
    }

    external fun nativeCreate(): Long
    external fun nativeDestroy(handle: Long)
    external fun nativeClear(handle: Long)
    /** Applies the original durable options; extra onsets follow through their own setter. */
    external fun nativeConfigure(
        handle: Long,
        method: Int,
        toneStyle: Int,
        smartRestore: Boolean,
        eagerRestore: Boolean,
        spellCheck: Boolean,
        autoCapitalize: Boolean,
    )

    /** Extra onset wire bits (F=1, J=2, W=4, Z=8); applied after every nativeConfigure. */
    external fun nativeSetExtraOnsets(handle: Long, letters: Int)

    /** Runtime VI/EN state — flipped per field and by the language key, not durable config. */
    external fun nativeSetEnabled(handle: Long, enabled: Boolean)
    external fun nativeClearShortcuts(handle: Long)
    external fun nativeAddShortcut(handle: Long, trigger: String, expansion: String)
    external fun nativeSetShortcutsEnabled(handle: Long, enabled: Boolean)
    external fun nativeSetShortcutSmartCase(handle: Long, enabled: Boolean)
    external fun nativeSetShortcutsInEnglish(handle: Long, enabled: Boolean)
    /** Re-opens an already-committed word for editing; false when it is not adoptable. */
    external fun nativeAdopt(handle: Long, word: String): Boolean

    external fun nativeProcess(handle: Long, codePoint: Int): String
    external fun nativeBoundary(handle: Long, codePoint: Int): String
    external fun nativeBackspace(handle: Long): String

    /**
     * Whether a caret sitting after [before] starts a sentence, by the rules shared
     * with every other Funput platform. Stateless, hence no handle.
     *
     * Always applies the typing reading, which treats a repeated full stop as an
     * abbreviation (`v.v. ` does not start a sentence, `TS. ` still does).
     */
    external fun nativeStartsSentence(before: String): Boolean

    /** Whether a caret sitting after [before] starts a word. Stateless, hence no handle. */
    external fun nativeStartsWord(before: String): Boolean

    // Typo correction: see `nativebridge/correction/TypoCorrectionEngine` for the contract.
    external fun nativeSetTypoCorrection(handle: Long, on: Boolean)
    external fun nativeSetNextKeyTouch(
        handle: Long, typed: Int, typedDistance: Float,
        a1: Int, d1: Float, a2: Int, d2: Float, a3: Int, d3: Float,
    )
    external fun nativeHasPendingCorrection(handle: Long): Boolean
    external fun nativeCorrectionCandidates(handle: Long): Array<String>?
    external fun nativePendingCorrectionBackspace(handle: Long): Int
    external fun nativeChooseCorrection(handle: Long, uses: IntArray?, allowed: BooleanArray?): Int
    external fun nativeApplyCorrection(handle: Long, index: Int): IntArray?
    external fun nativeHasCorrectionUndo(handle: Long): Boolean
    external fun nativeCorrectionUndoText(handle: Long): String
    external fun nativeUndoCorrection(handle: Long): IntArray?
    external fun nativePreviewCorrection(handle: Long): Int
    external fun nativeRefuseCorrection(handle: Long)
    external fun nativeRankCorrection(handle: Long, uses: IntArray?, allowed: BooleanArray?): IntArray?
    external fun nativeCorrectionMetrics(handle: Long): IntArray?
}
