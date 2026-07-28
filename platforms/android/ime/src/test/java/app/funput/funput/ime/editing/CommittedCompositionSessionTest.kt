package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
import app.funput.funput.keyboard.model.KeyAction
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommittedCompositionSessionTest {
    @Test
    fun `typing and backspace replace committed buffer without composing spans`() {
        val editor = CommittedEditor()
        val engine = ScriptedEngine(
            processed = ArrayDeque(listOf("a", "an", "án")),
            backspaceOutput = "an",
        )
        val session = testSession(engine).apply {
            setRenderMode(CompositionRenderMode.COMMITTED)
        }

        "ans".forEach { session.input(editor.proxy, it.toString()) }
        assertTrue(session.backspace(editor.proxy))

        assertEquals("an", editor.text)
        assertEquals(0, editor.setComposingCount)
        assertEquals("an", session.composingText)
    }

    @Test
    fun `selection update from committed replacement keeps engine buffer`() {
        val editor = CommittedEditor()
        val session = testSession(ScriptedEngine(ArrayDeque(listOf("chào"))))
        val handler = ImeKeyActionHandler(
            composition = session,
            editor = InputConnectionEditor(),
            connection = { editor.proxy },
            enterCommand = { ImeEditCommand.CommitText("\n") },
        )
        handler.start(renderMode = CompositionRenderMode.COMMITTED)

        handler.onKeyAction(KeyAction.Input(keyId = "character-a", text = "a"))
        handler.onSelectionChanged(newStart = 4, newEnd = 4, composingEnd = -1)

        assertTrue(session.isComposing)
        assertEquals("chào", editor.text)
    }

    @Test
    fun `boundary replacement rewrites committed word once`() {
        val editor = CommittedEditor()
        val session = testSession(
            ScriptedEngine(ArrayDeque(listOf("cảd")), boundaryOutput = "card "),
        ).apply {
            setRenderMode(CompositionRenderMode.COMMITTED)
        }

        session.input(editor.proxy, "d")
        session.input(editor.proxy, " ")

        assertEquals("card ", editor.text)
        assertEquals(0, editor.setComposingCount)
    }

    @Test
    fun `backspace reopens committed word so next tone key can edit it`() {
        val editor = CommittedEditor(exposesSurroundingText = false)
        val engine = ScriptedEngine(ArrayDeque(listOf("chào", "cháo"))).apply {
            adoptable = setOf("chào")
        }
        val session = testSession(engine)
        val handler = ImeKeyActionHandler(
            composition = session,
            editor = InputConnectionEditor(),
            connection = { editor.proxy },
            enterCommand = { ImeEditCommand.CommitText("\n") },
        )
        handler.start(renderMode = CompositionRenderMode.COMMITTED)

        session.input(editor.proxy, "a")
        session.input(editor.proxy, " ")
        handler.onKeyAction(KeyAction.Backspace)
        handler.onSelectionChanged(newStart = 4, newEnd = 4, composingEnd = -1)
        handler.onKeyAction(KeyAction.Input(keyId = "character-s", text = "s"))

        assertEquals("cháo", editor.text)
        assertEquals(0, editor.setComposingCount)
    }
}

private class CommittedEditor(
    var text: String = "",
    private val exposesSurroundingText: Boolean = true,
) {
    var setComposingCount = 0

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
                if (!exposesSurroundingText || count >= GraphemeLookback) null else text.takeLast(count)
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
