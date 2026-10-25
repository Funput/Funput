package app.funput.funput.ime.shortcuts.undo

import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.CommittedEditor
import app.funput.funput.ime.nativebridge.EngineConfiguration
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.ime.nativebridge.correction.CorrectionEdit
import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutUndoTest {
    @Test fun backspacePutsTheTriggerBackAndKeepsTheSpace() {
        val undo = armed()
        val editor = CommittedEditor(text = "việt nam ")
        val shortcut = ShortcutUndo(session(undo)) { editor.proxy }

        assertEquals("vn", shortcut.chipText())
        assertTrue(shortcut.undo())

        assertEquals("vn ", editor.text)
        assertNull(shortcut.chipText())
        assertFalse(undo.dismissed)
    }

    @Test fun aDifferentSuffixDropsTheBeatAndDeletesNothing() {
        val undo = armed()
        val editor = CommittedEditor(text = "khác ")
        val shortcut = ShortcutUndo(session(undo)) { editor.proxy }

        assertNull(shortcut.chipText())
        assertFalse(shortcut.undo())

        assertEquals("khác ", editor.text)
        assertTrue(undo.dismissed)
    }

    @Test fun anUnreadableDocumentHidesTheChipWithoutDroppingTheBeat() {
        val undo = armed()
        val editor = CommittedEditor(text = "việt nam ", exposesSurroundingText = false)
        val shortcut = ShortcutUndo(session(undo)) { editor.proxy }

        assertNull(shortcut.chipText())
        assertFalse(undo.dismissed)
    }

    private fun armed() = MemoryUndo()

    private fun session(undo: MemoryUndo) = AndroidCompositionSession(UndoEngine(undo))
}

private class MemoryUndo : ShortcutUndoEngine {
    var shown: String? = "vn"
    var written: String? = "việt nam "
    var dismissed = false

    override fun isArmed() = shown != null
    override fun text() = shown
    override fun suffix() = written

    override fun undo(): CorrectionEdit? {
        val replacement = (shown ?: return null) + (written?.last() ?: return null)
        val backspace = written?.length ?: return null
        shown = null
        written = null
        return CorrectionEdit(backspace, replacement)
    }

    override fun dismiss() {
        dismissed = true
        shown = null
        written = null
    }
}

private class UndoEngine(override val shortcutUndo: ShortcutUndoEngine) : VietnameseEngine {
    override val inputMethod = KeyboardInputMethod.TELEX
    override fun configure(configuration: EngineConfiguration) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
    override fun adopt(word: String) = false
    override fun process(codePoint: Int) = ""
    override fun processBoundary(codePoint: Int): String? = null
    override fun backspace() = ""
    override fun clear() = Unit
    override fun close() = Unit
}
