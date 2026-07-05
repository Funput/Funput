package app.funput.funput.ime.nativebridge

/** Narrow JNI surface. All calls are synchronous on the IME main thread. */
internal object FunputNative {
    init {
        System.loadLibrary("funput_jni")
    }

    external fun nativeCreate(): Long
    external fun nativeDestroy(handle: Long)
    external fun nativeClear(handle: Long)
    external fun nativeSetMethod(handle: Long, method: Int)
    external fun nativeSetToneStyle(handle: Long, style: Int)
    external fun nativeSetEnabled(handle: Long, enabled: Boolean)
    external fun nativeSetSpellCheck(handle: Long, enabled: Boolean)
    external fun nativeSetSmartRestore(handle: Long, enabled: Boolean)
    external fun nativeSetEagerRestore(handle: Long, enabled: Boolean)
    external fun nativeProcess(handle: Long, codePoint: Int): String
    external fun nativeBoundary(handle: Long, codePoint: Int): String
    external fun nativeBackspace(handle: Long): String
}
