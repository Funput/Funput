package app.funput.funput.ime.editing

import app.funput.funput.keyboard.model.KeyAction
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

    @Test
    fun `withheld surrounding text retones after backspacing a refused coda`() {
        val editor = CommittedEditor(exposesSurroundingText = false)
        val engine = ScriptedEngine(
            processed = ArrayDeque(listOf("d", "du", "dun", "dung", "dungh", "dúng")),
        ).apply { adoptable = setOf("dung") }
        val session = testSession(engine)
        val handler = ImeKeyActionHandler(
            composition = session,
            editor = InputConnectionEditor(),
            connection = { editor.proxy },
            enterCommand = { ImeEditCommand.CommitText("\n") },
        )
        handler.start(renderMode = CompositionRenderMode.COMMITTED)

        "dungh".forEach { session.input(editor.proxy, it.toString()) }
        session.input(editor.proxy, " ")
        handler.onKeyAction(KeyAction.Backspace)
        handler.onSelectionChanged(newStart = 5, newEnd = 5, composingEnd = -1)
        assertEquals("dungh", editor.text)
        assertEquals("", session.composingText)

        handler.onKeyAction(KeyAction.Backspace)
        handler.onSelectionChanged(newStart = 4, newEnd = 4, composingEnd = -1)
        handler.onKeyAction(KeyAction.Input(keyId = "character-s", text = "s"))

        assertEquals("dúng", editor.text)
        assertEquals("dung", engine.adopted)
    }
}
