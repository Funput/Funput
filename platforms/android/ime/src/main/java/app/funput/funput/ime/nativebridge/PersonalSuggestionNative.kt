package app.funput.funput.ime.nativebridge

internal object PersonalSuggestionNative {
    init {
        System.loadLibrary("funput_jni")
    }

    external fun nativeCreate(): Long
    external fun nativeOpen(path: String): Long
    external fun nativeAttachLexicon(handle: Long, path: String): Boolean
    external fun nativeDestroy(handle: Long)
    external fun nativeLearn(handle: Long, token: String): Boolean
    external fun nativeLearnAfter(handle: Long, previous: String, token: String): Boolean
    external fun nativeQuery(handle: Long, prefix: String): Array<String>?
    external fun nativeQueryWith(handle: Long, previous: String, prefix: String): Array<String>?
    external fun nativeFlush(handle: Long): Boolean
    external fun nativeCompact(handle: Long): Boolean
    external fun nativeReset(handle: Long): Boolean
    external fun nativeStats(handle: Long): LongArray?

    /** True for a word the user typed, the English list holds, or the keep list protects. */
    external fun nativeIsKnownWord(handle: Long, word: String): Boolean

    /** The shipped syllable tier as pseudo-uses: 4, 3, 2, 1, or 0 when unlisted. No handle. */
    external fun nativeSyllableWeight(word: String): Int
}
