package app.funput.funput.ime.editing.backspace

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.ImeKeyActionHandler
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.VietnameseEngine
import java.lang.reflect.Proxy

internal fun retoneHandler(
    session: AndroidCompositionSession,
    editor: MutableEditor,
) = ImeKeyActionHandler(
    composition = session,
    editor = InputConnectionEditor(),
    connection = { editor.proxy },
    enterCommand = { ImeEditCommand.CommitText("\n") },
)

/**
 * Returns empty for the grapheme lookback used by the JVM-incompatible
 * BreakIterator path, while preserving the shorter word lookback.
 */
internal class MutableEditor(var text: String) {
    var batchDepth = 0
        private set
    var deleteOccurredInsideOuterBatch = false
        private set
    var finishCount = 0
        private set
    private var sawOuterBatch = false

    val proxy: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "getTextBeforeCursor" -> {
                val count = arguments?.first() as Int
                if (count >= GraphemeLookback) "" else text.takeLast(count)
            }
            "getSelectedText" -> null
            "deleteSurroundingText" -> delete(arguments?.first() as Int)
            "deleteSurroundingTextInCodePoints" -> deleteCodePoints(arguments?.first() as Int)
            "setComposingText" -> true.also {
                text = (arguments?.first() as CharSequence).toString()
            }
            "beginBatchEdit" -> true.also {
                batchDepth += 1
                if (batchDepth == 1) sawOuterBatch = true
            }
            "endBatchEdit" -> true.also { batchDepth -= 1 }
            "finishComposingText" -> true.also { finishCount++ }
            "commitText" -> true.also { text += arguments?.first().toString() }
            "toString" -> "MutableEditor"
            else -> false
        }
    } as InputConnection

    private fun delete(before: Int): Boolean {
        recordDeleteInBatch()
        if (before > 0 && text.length >= before) text = text.dropLast(before)
        return true
    }

    private fun deleteCodePoints(before: Int): Boolean {
        recordDeleteInBatch()
        repeat(before) {
            if (text.isEmpty()) return@repeat
            val codePoint = text.codePointBefore(text.length)
            text = text.substring(0, text.length - Character.charCount(codePoint))
        }
        return true
    }

    private fun recordDeleteInBatch() {
        if (sawOuterBatch && batchDepth > 0) deleteOccurredInsideOuterBatch = true
    }

    private companion object {
        const val GraphemeLookback = 128
    }
}

internal class RetoneAdoptingEngine(private val adoptable: Set<String>) : VietnameseEngine {
    override fun adopt(word: String): Boolean = adoptable.contains(word)
    override fun process(codePoint: Int): String = ""
    override fun processBoundary(codePoint: Int): String? = null
    override fun backspace(): String = ""
    override fun configure(configuration: EngineConfiguration) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
    override fun clear() = Unit
    override fun close() = Unit
}
