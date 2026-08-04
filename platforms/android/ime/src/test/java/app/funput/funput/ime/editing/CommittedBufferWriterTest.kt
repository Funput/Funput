package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommittedBufferWriterTest {
    @Test
    fun `prefix growth commits only the suffix`() {
        val editor = BrokenDeleteEditor()
        val writer = CommittedBufferWriter()

        assertTrue(writer.replace(editor.proxy, "", "p", deleteWithKeyEvents = false))
        assertTrue(writer.replace(editor.proxy, "p", "ph", deleteWithKeyEvents = false))
        assertTrue(writer.replace(editor.proxy, "ph", "phu", deleteWithKeyEvents = false))

        assertEquals("phu", editor.text)
        assertEquals(0, editor.deleteCalls)
    }

    @Test
    fun `tone transform deletes with key events when surrounding delete is ignored`() {
        val editor = BrokenDeleteEditor()
        val writer = CommittedBufferWriter()

        writer.replace(editor.proxy, "", "a", deleteWithKeyEvents = true)
        writer.replace(editor.proxy, "a", "á", deleteWithKeyEvents = true)

        assertEquals("á", editor.text)
        assertEquals(0, editor.deleteCalls)
        assertEquals(1, editor.keyDeleteCount)
    }

    @Test
    fun `key-delete session types phu without duplication`() {
        val editor = BrokenDeleteEditor()
        val engine = ScriptedEngine(ArrayDeque(listOf("p", "ph", "phu")))
        val session = testSession(engine).apply {
            setRenderMode(CompositionRenderMode.COMMITTED_KEY_DELETE)
        }

        "phu".forEach { session.input(editor.proxy, it.toString()) }

        assertEquals("phu", editor.text)
        assertEquals("phu", session.composingText)
    }
}

/** Mimics ONLYOFFICE: deleteSurroundingText returns true but does not remove text. */
private class BrokenDeleteEditor(var text: String = "") {
    var deleteCalls = 0
    var keyDeleteCount = 0
    private var keyEventCount = 0

    val proxy: InputConnection = Proxy.newProxyInstance(
        InputConnection::class.java.classLoader,
        arrayOf(InputConnection::class.java),
    ) { _, method, arguments ->
        when (method.name) {
            "beginBatchEdit", "endBatchEdit" -> true
            "commitText" -> true.also { text += arguments?.first().toString() }
            "deleteSurroundingText" -> true.also { deleteCalls += 1 }
            "sendKeyEvent" -> true.also {
                // Android stubs block KeyEvent getters; pair DOWN+UP as one delete.
                keyEventCount += 1
                if (keyEventCount % 2 == 0) {
                    keyDeleteCount += 1
                    if (text.isNotEmpty()) text = text.dropLast(1)
                }
            }
            "getSelectedText" -> null
            "getTextBeforeCursor" -> null
            "setComposingText" -> true
            "toString" -> "BrokenDeleteEditor"
            else -> false
        }
    } as InputConnection
}
