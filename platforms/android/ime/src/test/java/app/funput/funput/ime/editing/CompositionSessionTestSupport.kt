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

/**
 * Committed-mode host that can hide surrounding text the way Reddit and similar
 * editors do: deletes still apply, but [InputConnection.getTextBeforeCursor] is null.
 */
internal class CommittedEditor(
    var text: String = "",
    private val exposesSurroundingText: Boolean = true,
) {
    var setComposingCount = 0
    /**
     * What `getTextBeforeCursor` answers when the document tail is not what a read sees:
     * a host still showing the state between our delete and our commit, or a caret this
     * fake does not otherwise model.
     */
    var textBeforeCursor: String? = null

    val proxy: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "beginBatchEdit", "endBatchEdit" -> true
            "commitText" -> true.also { text += arguments?.first().toString() }
            "deleteSurroundingText" -> true.also {
                text = text.dropLast(arguments?.first() as Int)
            }
            "getSelectedText" -> null
            "getTextBeforeCursor" -> {
                val count = arguments?.first() as Int
                if (!exposesSurroundingText || count >= GraphemeLookback) {
                    null
                } else {
                    (textBeforeCursor ?: text).takeLast(count)
                }
            }
            "deleteSurroundingTextInCodePoints" -> true.also {
                repeat(arguments?.first() as Int) {
                    if (text.isNotEmpty()) text = text.dropLast(1)
                }
            }
            "setComposingText" -> true.also { setComposingCount++ }
            "toString" -> "CommittedEditor"
            else -> false
        }
    } as InputConnection

    private companion object {
        const val GraphemeLookback = 128
    }
}
