package app.funput.funput.ime.shortcuts

import app.funput.funput.ime.editing.CommittedEditor
import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.shortcuts.model.ShortcutLibrary
import app.funput.funput.shortcuts.model.TextShortcut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeShortcutSessionTest {
    @Test fun `english trigger expands on a boundary`() {
        val engine = ShortcutEngine("việt nam ")
        val session = ImeShortcutSession(engine)
        val editor = CommittedEditor()
        session.receive(library(), wordIsActive = false)

        assertFalse(session.inputEnglish(editor.proxy, "v"))
        assertFalse(session.inputEnglish(editor.proxy, "n"))
        assertTrue(session.inputEnglish(editor.proxy, " "))

        assertEquals("việt nam ", editor.text)
    }

    @Test fun `unsafe context keeps the trigger and boundary`() {
        val session = ImeShortcutSession(ShortcutEngine("việt nam "))
        val editor = CommittedEditor()
        session.receive(library(), wordIsActive = false)
        session.inputEnglish(editor.proxy, "v")
        session.inputEnglish(editor.proxy, "n")
        editor.textBeforeCursor = "other"

        assertFalse(session.inputEnglish(editor.proxy, " "))
        assertEquals("vn ", editor.text)
    }

    @Test fun `late library waits until the active word ends`() {
        val engine = ShortcutEngine("new ")
        val session = ImeShortcutSession(engine)
        session.beginActivation()
        session.receive(library(), wordIsActive = true)
        assertEquals(0, engine.installs)
        session.finishWord()
        assertEquals(1, engine.installs)
    }

    @Test fun `activation clears the previous native table`() {
        val engine = ShortcutEngine(null)
        val session = ImeShortcutSession(engine)
        session.receive(library(), wordIsActive = false)
        assertTrue(session.runsInEnglish)
        session.beginActivation()
        assertFalse(session.runsInEnglish)
        assertEquals(1, engine.clears)
    }

    private fun library() = ShortcutLibrary(entries = listOf(
        TextShortcut(trigger = "vn", expansion = "việt nam"),
    ))
}

private class ShortcutEngine(private val boundary: String?) : VietnameseEngine {
    private var word = ""
    var installs = 0
    var clears = 0
    override val inputMethod = KeyboardInputMethod.TELEX
    override fun configure(configuration: EngineConfiguration) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
    override fun installShortcuts(library: ShortcutLibrary) { installs += 1 }
    override fun clearShortcuts() { clears += 1 }
    override fun adopt(word: String) = false
    override fun process(codePoint: Int): String = (word + String(Character.toChars(codePoint)))
        .also { word = it }
    override fun processBoundary(codePoint: Int): String? = boundary.also { word = "" }
    override fun backspace(): String = word.dropLast(1).also { word = it }
    override fun clear() { word = "" }
    override fun close() = Unit
}
