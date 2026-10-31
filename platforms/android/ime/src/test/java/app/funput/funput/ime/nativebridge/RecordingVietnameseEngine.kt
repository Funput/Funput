package app.funput.funput.ime.nativebridge

/**
 * A composer that records what it was told and composes nothing. Shared by the tests
 * that only care which configuration and Vietnamese switch reached the engine.
 */
internal class RecordingVietnameseEngine : VietnameseEngine {
    val configurations = mutableListOf<EngineConfiguration>()
    val enabledCalls = mutableListOf<Boolean>()
    var processCount = 0
        private set

    /** The last switch, or the native engine's own default before any call. */
    val enabledValue: Boolean get() = enabledCalls.lastOrNull() ?: true

    override fun configure(configuration: EngineConfiguration) {
        configurations += configuration
    }

    override fun setEnabled(enabled: Boolean) {
        enabledCalls += enabled
    }

    override fun process(codePoint: Int): String = "".also { processCount++ }
    override fun processBoundary(codePoint: Int): String? = null
    override fun backspace(): String = ""
    override fun adopt(word: String): Boolean = false
    override fun clear() = Unit
    override fun close() = Unit
}
