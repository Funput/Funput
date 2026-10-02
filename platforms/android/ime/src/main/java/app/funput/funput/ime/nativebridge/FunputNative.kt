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
}
