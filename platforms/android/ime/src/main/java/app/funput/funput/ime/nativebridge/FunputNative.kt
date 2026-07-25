package app.funput.funput.ime.nativebridge

/** Narrow JNI surface. All calls are synchronous on the IME main thread. */
internal object FunputNative {
    init {
        System.loadLibrary("funput_jni")
    }

    external fun nativeCreate(): Long
    external fun nativeDestroy(handle: Long)
    external fun nativeClear(handle: Long)
    /** Applies every durable engine option in one crossing (see [EngineConfiguration]). */
    external fun nativeConfigure(
        handle: Long,
        method: Int,
        toneStyle: Int,
        smartRestore: Boolean,
        eagerRestore: Boolean,
        spellCheck: Boolean,
        autoCapitalize: Boolean,
    )

    /** Runtime VI/EN state — flipped per field and by the language key, not durable config. */
    external fun nativeSetEnabled(handle: Long, enabled: Boolean)
    /** Re-opens an already-committed word for editing; false when it is not adoptable. */
    external fun nativeAdopt(handle: Long, word: String): Boolean

    external fun nativeProcess(handle: Long, codePoint: Int): String
    external fun nativeBoundary(handle: Long, codePoint: Int): String
    external fun nativeBackspace(handle: Long): String
}
