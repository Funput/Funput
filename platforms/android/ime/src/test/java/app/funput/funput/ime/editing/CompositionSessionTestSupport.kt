package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.keyboard.model.KeyboardInputMethod
import java.lang.reflect.Proxy

internal fun testSession(engine: VietnameseEngine) = AndroidCompositionSession(
    engine = engine,
    composingTextFactory = { text -> text },
)

internal class ScriptedEngine(
    private val processed: ArrayDeque<String>,
    private val boundaryOutput: String? = null,
    private val backspaceOutput: String = "",
    override var inputMethod: KeyboardInputMethod = KeyboardInputMethod.TELEX,
) : VietnameseEngine {
    var configuration: EngineConfiguration? = null
        private set
    var enabled: Boolean? = null
        private set
    /** Words this fake accepts for re-opening; mirrors the engine's syllable gate. */
    var adoptable: Set<String> = emptySet()
    var adopted: String? = null
        private set

    override fun process(codePoint: Int): String = processed.removeFirst()
    override fun processBoundary(codePoint: Int): String? = boundaryOutput
    override fun backspace(): String = backspaceOutput
    override fun configure(configuration: EngineConfiguration) {
        this.configuration = configuration
        inputMethod = configuration.inputMethod
    }
    override fun adopt(word: String): Boolean = adoptable.contains(word).also {
        if (it) adopted = word
    }
    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
    override fun clear() = Unit
    override fun close() = Unit
}

internal class RecordingConnection {
    val composingTexts = mutableListOf<String>()
    val committedTexts = mutableListOf<String>()
    var finishCount = 0

    val proxy: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "setComposingText" -> true.also {
                val value = arguments?.first() as CharSequence
                composingTexts += value.toString()
            }
            "commitText" -> true.also { committedTexts += arguments?.first().toString() }
            "finishComposingText" -> true.also { finishCount++ }
            "toString" -> "RecordingInputConnection"
            else -> false
        }
    } as InputConnection
}
