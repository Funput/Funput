package app.funput.funput.ime.nativebridge

import app.funput.funput.keyboard.model.KeyboardInputMethod

/** Platform-independent contract consumed by Android's composition adapter. */
internal interface VietnameseEngine : AutoCloseable {
    fun setInputMethod(method: KeyboardInputMethod)
    fun setEnabled(enabled: Boolean)
    fun process(codePoint: Int): String
    fun processBoundary(codePoint: Int): String?
    fun backspace(): String
    fun clear()
}

/** Owns one safe Rust engine handle for the active IME service. */
internal class NativeVietnameseEngine : VietnameseEngine {
    private var handle = FunputNative.nativeCreate().also {
        check(it != InvalidHandle) { "Unable to create Funput native engine" }
    }

    override fun setInputMethod(method: KeyboardInputMethod) = withHandle { value ->
        FunputNative.nativeSetMethod(value, method.nativeValue)
    }

    override fun setEnabled(enabled: Boolean) = withHandle { value ->
        FunputNative.nativeSetEnabled(value, enabled)
    }

    override fun process(codePoint: Int): String = withHandle { value ->
        FunputNative.nativeProcess(value, codePoint)
    }

    override fun processBoundary(codePoint: Int): String? = withHandle { value ->
        FunputNative.nativeBoundary(value, codePoint).ifEmpty { null }
    }

    override fun backspace(): String = withHandle(FunputNative::nativeBackspace)

    override fun clear() = withHandle(FunputNative::nativeClear)

    override fun close() {
        if (handle == InvalidHandle) return
        FunputNative.nativeDestroy(handle)
        handle = InvalidHandle
    }

    private inline fun <T> withHandle(operation: (Long) -> T): T {
        check(handle != InvalidHandle) { "Funput native engine is closed" }
        return operation(handle)
    }

    private val KeyboardInputMethod.nativeValue: Int
        get() = when (this) {
            KeyboardInputMethod.TELEX -> 0
            KeyboardInputMethod.VNI -> 1
        }

    private companion object {
        const val InvalidHandle = 0L
    }
}
