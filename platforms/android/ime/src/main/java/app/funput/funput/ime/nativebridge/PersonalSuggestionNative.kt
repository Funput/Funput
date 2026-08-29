package app.funput.funput.ime.nativebridge

internal object PersonalSuggestionNative {
    init {
        System.loadLibrary("funput_jni")
    }

    external fun nativeCreate(): Long
    external fun nativeOpen(path: String): Long
    external fun nativeDestroy(handle: Long)
    external fun nativeLearn(handle: Long, token: String): Boolean
    external fun nativeLearnAfter(handle: Long, previous: String, token: String): Boolean
    external fun nativeQuery(handle: Long, prefix: String): Array<String>?
    external fun nativeQueryWith(handle: Long, previous: String, prefix: String): Array<String>?
    external fun nativeFlush(handle: Long): Boolean
    external fun nativeCompact(handle: Long): Boolean
    external fun nativeReset(handle: Long): Boolean
    external fun nativeStats(handle: Long): LongArray?
}
