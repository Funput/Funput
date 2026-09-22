package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.VietnameseEngine
import java.lang.reflect.Proxy

/** Editor stub covering the calls the re-open path makes. */
internal class ReopenWordEditor(
    textBeforeCursor: String,
    private val selectedText: String? = null,
    private val setComposingTextFails: Boolean = false,
) {
    var text = textBeforeCursor
        private set
    val composingTexts = mutableListOf<String>()
    val committedTexts = mutableListOf<String>()
    val deletedBefore = mutableListOf<Int>()
    var batchDepthPeak = 0
        private set
    private var batchDepth = 0

    val proxy: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "getTextBeforeCursor" -> text.takeLast(arguments?.first() as Int)
            "getSelectedText" -> selectedText
            "deleteSurroundingText" -> true.also {
                val count = arguments?.first() as Int
                deletedBefore += count
                text = text.dropLast(count)
            }
            "setComposingText" -> if (setComposingTextFails) false else true.also {
                val value = (arguments?.first() as CharSequence).toString()
                composingTexts += value
                text += value
            }
            "commitText" -> true.also {
                val value = (arguments?.first() as CharSequence).toString()
                committedTexts += value
                text += value
            }
            "beginBatchEdit" -> true.also {
                batchDepth += 1
                batchDepthPeak = maxOf(batchDepthPeak, batchDepth)
            }
            "endBatchEdit" -> true.also { batchDepth -= 1 }
            "finishComposingText" -> true
            "toString" -> "ReopenWordEditor"
            else -> false
        }
    } as InputConnection
}

/** Engine stub whose `adopt` accepts a fixed word set, standing in for the syllable gate. */
internal class ReopenWordEngine(private val adoptable: Set<String>) : VietnameseEngine {
    var buffer = ""
        private set
    var adopted: String? = null
        private set

    override fun adopt(word: String): Boolean = adoptable.contains(word).also {
        if (it) {
            adopted = word
            buffer = word
        }
    }

    override fun process(codePoint: Int): String = "a"
    override fun processBoundary(codePoint: Int): String? = null
    override fun backspace(): String = ""
    override fun configure(configuration: EngineConfiguration) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
    override fun clear() { buffer = "" }
    override fun close() = Unit
}
