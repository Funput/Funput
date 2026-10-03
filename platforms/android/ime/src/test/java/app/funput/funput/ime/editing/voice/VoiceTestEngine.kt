package app.funput.funput.ime.editing.voice

import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.VietnameseEngine

internal class VoiceTestEngine : VietnameseEngine {
    var calls = 0
    override fun process(codePoint: Int): String = "".also { calls++ }
    override fun processBoundary(codePoint: Int): String? = null.also { calls++ }
    override fun backspace(): String = "".also { calls++ }
    override fun configure(configuration: EngineConfiguration) { calls++ }
    override fun adopt(word: String): Boolean = false.also { calls++ }
    override fun setEnabled(enabled: Boolean) { calls++ }
    override fun clear() { calls++ }
    override fun close() { calls++ }
}
