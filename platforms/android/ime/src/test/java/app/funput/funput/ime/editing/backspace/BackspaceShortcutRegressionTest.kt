package app.funput.funput.ime.editing.backspace

import app.funput.funput.ime.editing.CompositionRenderMode
import app.funput.funput.ime.editing.testSession
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.shortcuts.model.ShortcutLibrary
import app.funput.funput.shortcuts.model.TextShortcut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackspaceShortcutRegressionTest {
    @Test
    fun `successive backspaces preserve the reopened engine even with shortcuts disabled`() {
        for (allowShortcuts in listOf(false, true)) {
            for (mode in listOf(CompositionRenderMode.COMPOSING, CompositionRenderMode.COMMITTED)) {
                val editor = MutableEditor("chào ")
                val engine = RetoneAdoptingEngine(setOf("chào"))
                val session = testSession(engine)
                val handler = retoneHandler(session, editor)
                handler.start(allowShortcuts = allowShortcuts, renderMode = mode)

                handler.onKeyAction(KeyAction.Backspace)
                assertEquals("chào", session.composingText)
                assertEquals("chào", engine.buffer)

                for (expected in listOf("chà", "ch", "c", "")) {
                    handler.onKeyAction(KeyAction.Backspace)
                    assertEquals("$allowShortcuts / $mode", expected, editor.text)
                    assertEquals(expected, session.composingText)
                    assertEquals(expected, engine.buffer)
                }
                assertEquals(0, editor.batchDepth)
            }
        }
    }

    @Test
    fun `erasing a newly typed suffix does not clear the reopened word`() {
        val editor = MutableEditor("chào ")
        val engine = RetoneAdoptingEngine(setOf("chào"))
        val session = testSession(engine)
        val handler = retoneHandler(session, editor)
        handler.start()
        handler.onKeyAction(KeyAction.Backspace)
        handler.onKeyAction(KeyAction.Input("key-n", "n"))
        handler.onKeyAction(KeyAction.Backspace)

        assertEquals("chào", editor.text)
        assertEquals("chào", engine.buffer)
        handler.onKeyAction(KeyAction.Backspace)
        assertEquals("chà", editor.text)
    }

    @Test
    fun `late shortcuts wait for the reopened composition to end`() {
        for (finishWithSpace in listOf(false, true)) {
            val editor = MutableEditor("chào ")
            val engine = RetoneAdoptingEngine(setOf("chào"))
            val session = testSession(engine)
            val handler = retoneHandler(session, editor)
            val library = ShortcutLibrary(entries = listOf(TextShortcut(trigger = "vn", expansion = "Việt Nam")))
            handler.start()
            handler.onKeyAction(KeyAction.Backspace)
            handler.receiveShortcuts(library)
            assertTrue(engine.installedLibraries.isEmpty())

            handler.onKeyAction(KeyAction.Backspace)
            assertEquals("chà", editor.text)
            assertTrue(engine.installedLibraries.isEmpty())
            if (finishWithSpace) handler.onKeyAction(KeyAction.Space)
            else repeat(3) { handler.onKeyAction(KeyAction.Backspace) }

            assertEquals(listOf(library), engine.installedLibraries)
        }
    }
}
