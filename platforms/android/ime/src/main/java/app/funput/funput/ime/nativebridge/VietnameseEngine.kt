package app.funput.funput.ime.nativebridge

import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.model.KeyboardInputMethod

/**
 * The engine's durable options, applied as one batch by [VietnameseEngine.configure].
 *
 * [autoCapitalize] stays off on Android: sentence capitalization is handled at the
 * keyboard layer by `AutoCapitalizationController` (shift state driven by the editor's
 * `CAP_MODE_*` flags), so enabling it in the engine too would capitalize twice.
 */
internal data class EngineConfiguration(
    val inputMethod: KeyboardInputMethod,
    val toneStyle: ToneStyle,
    val smartRestore: Boolean,
    val eagerRestore: Boolean,
    val spellCheck: Boolean,
    val autoCapitalize: Boolean = false,
)

/** Platform-independent contract consumed by Android's composition adapter. */
internal interface VietnameseEngine : AutoCloseable {
    /** Single writer for durable configuration; see [EngineConfiguration]. */
    fun configure(configuration: EngineConfiguration)
    fun setEnabled(enabled: Boolean)

    /**
     * Re-opens an already-committed [word] as the live composition so the next
     * keystroke edits it. Returns false when the word is not a Vietnamese syllable —
     * the caller must then leave the document alone.
     */
    fun adopt(word: String): Boolean
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

    override fun configure(configuration: EngineConfiguration) = withHandle { value ->
        FunputNative.nativeConfigure(
            value,
            configuration.inputMethod.nativeValue,
            configuration.toneStyle.nativeValue,
            configuration.smartRestore,
            configuration.eagerRestore,
            configuration.spellCheck,
            configuration.autoCapitalize,
        )
    }

    override fun setEnabled(enabled: Boolean) = withHandle { value ->
        FunputNative.nativeSetEnabled(value, enabled)
    }

    override fun adopt(word: String): Boolean = withHandle { value ->
        FunputNative.nativeAdopt(value, word)
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
