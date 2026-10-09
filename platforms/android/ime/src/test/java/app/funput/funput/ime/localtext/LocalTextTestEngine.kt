package app.funput.funput.ime.localtext

import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.model.KeyboardInputMethod

/**
 * A tiny Telex for exercising the field's bookkeeping: `s` after a vowel adds the acute,
 * `dd` becomes `đ`, `q` is a key the engine will not compose, the rest is kept as typed.
 * Real composition is covered by the Rust engine's own tests.
 */
internal class LocalTextTestEngine : VietnameseEngine {
    override var inputMethod = KeyboardInputMethod.TELEX
        private set
    var configuration: EngineConfiguration? = null
        private set
    var enabled: Boolean? = null
        private set
    var clears = 0
        private set
    var keyCalls = 0
        private set
    var closes = 0
        private set
    /** What the next boundary hands back, boundary included; null keeps the word as typed. */
    var boundaryReplacement: String? = null
    var adoptable: Set<String> = emptySet()
    private var buffer = ""

    override fun process(codePoint: Int): String {
        keyCalls++
        val key = codePoint.toChar()
        val last = buffer.lastOrNull()
        buffer = when {
            key == 'q' -> ""
            key == 's' && last != null && last in Acute -> buffer.dropLast(1) + Acute.getValue(last)
            key == 'd' && last == 'd' -> buffer.dropLast(1) + "đ"
            else -> buffer + key
        }
        return buffer
    }

    override fun processBoundary(codePoint: Int): String? {
        keyCalls++
        buffer = ""
        return boundaryReplacement
    }

    override fun backspace(): String {
        keyCalls++
        buffer = buffer.dropLast(1)
        return buffer
    }

    override fun adopt(word: String): Boolean = (word in adoptable).also { if (it) buffer = word }

    override fun configure(configuration: EngineConfiguration) {
        this.configuration = configuration
        inputMethod = configuration.inputMethod
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun clear() {
        clears++
        buffer = ""
    }

    override fun close() {
        closes++
    }

    private companion object {
        val Acute = mapOf('a' to 'á', 'e' to 'é', 'o' to 'ó')
    }
}

internal fun engineConfiguration(method: KeyboardInputMethod) = EngineConfiguration(
    inputMethod = method,
    toneStyle = ToneStyle.TRADITIONAL,
    smartRestore = true,
    eagerRestore = true,
    spellCheck = false,
)
