package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.keyboard.model.KeyAction
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Retone-after-Backspace through [ImeKeyActionHandler].
 *
 * Real-device failure: DeleteBackward and reopenPreviousWord were two top-level
 * edits. EditText posted onUpdateSelection for the delete with candidatesEnd=-1
 * *after* reopen had set isComposing, and [ImeKeyActionHandler.onSelectionChanged]
 * finished the restored composition. Unit tests never called onSelectionChanged,
 * so they stayed green.
 */
class RetoneAfterBackspaceTest {
    @Test
    fun `backspace delete and reopen share one outer batch edit`() {
        val editor = MutableEditor(text = "chào ")
        val engine = RetoneAdoptingEngine(adoptable = setOf("chào"))
        val session = testSession(engine)
        val handler = retoneHandler(session, editor)

        handler.start(allowComposition = true)
        handler.onKeyAction(KeyAction.Backspace)

        assertEquals("chào", session.composingText)
        assertEquals("chào", editor.text)
        assertTrue(
            "DeleteBackward and reopen must nest under one outer beginBatchEdit " +
                "so EditText posts a single onUpdateSelection with the composing " +
                "region already set (candidatesEnd == caret).",
            editor.deleteOccurredInsideOuterBatch,
        )
        assertEquals(0, editor.batchDepth)
    }

    @Test
    fun `selection update matching the composing end keeps the reopened word`() {
        val editor = MutableEditor(text = "chào ")
        val session = testSession(RetoneAdoptingEngine(adoptable = setOf("chào")))
        val handler = retoneHandler(session, editor)

        handler.start(allowComposition = true)
        handler.onKeyAction(KeyAction.Backspace)
        // Final state after one batched edit: caret and composing end coincide.
        handler.onSelectionChanged(newStart = 4, newEnd = 4, composingEnd = 4)

        assertTrue(session.isComposing)
        assertEquals("chào", session.composingText)
    }

    @Test
    fun `stale selection update with cleared composing region finishes the word`() {
        val editor = MutableEditor(text = "chào ")
        val session = testSession(RetoneAdoptingEngine(adoptable = setOf("chào")))
        val handler = retoneHandler(session, editor)

        handler.start(allowComposition = true)
        handler.onKeyAction(KeyAction.Backspace)
        // What arrives when DeleteBackward is its own top-level edit: the delete's
        // onUpdateSelection is delivered after reopen has set isComposing.
        handler.onSelectionChanged(newStart = 4, newEnd = 4, composingEnd = -1)

        assertFalse(session.isComposing)
        assertEquals(1, editor.finishCount)
    }
}

private fun retoneHandler(
    session: AndroidCompositionSession,
    editor: MutableEditor,
) = ImeKeyActionHandler(
    composition = session,
    editor = InputConnectionEditor(),
    connection = { editor.proxy },
    enterCommand = { ImeEditCommand.CommitText("\n") },
)

/**
 * Mutable InputConnection double.
 *
 * [InputConnectionEditor.deleteBackward] uses `BreakIterator`, which is unavailable
 * on plain JVM android stubs. Returning empty for the grapheme lookback (128)
 * forces the code-point fallback; [wordBeforeCursor] uses lookback 12 and still
 * sees the real tail after the space is removed.
 */
private class MutableEditor(var text: String) {
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
                val n = arguments?.first() as Int
                if (n >= GraphemeLookback) "" else text.takeLast(n)
            }
            "getSelectedText" -> null
            "deleteSurroundingText" -> {
                val before = arguments?.first() as Int
                recordDeleteInBatch()
                if (before > 0 && text.length >= before) {
                    text = text.dropLast(before)
                }
                true
            }
            "deleteSurroundingTextInCodePoints" -> {
                val before = arguments?.first() as Int
                recordDeleteInBatch()
                repeat(before) {
                    if (text.isEmpty()) return@repeat
                    val cp = text.codePointBefore(text.length)
                    text = text.substring(0, text.length - Character.charCount(cp))
                }
                true
            }
            "setComposingText" -> {
                text = (arguments?.first() as CharSequence).toString()
                true
            }
            "beginBatchEdit" -> {
                batchDepth += 1
                if (batchDepth == 1) sawOuterBatch = true
                true
            }
            "endBatchEdit" -> {
                batchDepth -= 1
                true
            }
            "finishComposingText" -> true.also { finishCount++ }
            "commitText" -> {
                text += arguments?.first().toString()
                true
            }
            "toString" -> "MutableEditor"
            else -> false
        }
    } as InputConnection

    private fun recordDeleteInBatch() {
        if (sawOuterBatch && batchDepth > 0) deleteOccurredInsideOuterBatch = true
    }

    private companion object {
        // Mirrors InputConnectionEditor.GraphemeLookback.
        const val GraphemeLookback = 128
    }
}

private class RetoneAdoptingEngine(private val adoptable: Set<String>) : VietnameseEngine {
    override fun adopt(word: String): Boolean = adoptable.contains(word)
    override fun process(codePoint: Int): String = ""
    override fun processBoundary(codePoint: Int): String? = null
    override fun backspace(): String = ""
    override fun configure(configuration: EngineConfiguration) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
    override fun clear() = Unit
    override fun close() = Unit
}
