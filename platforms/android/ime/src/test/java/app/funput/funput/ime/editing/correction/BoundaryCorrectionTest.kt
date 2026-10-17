package app.funput.funput.ime.editing.correction

import app.funput.funput.ime.editing.CommittedEditor
import app.funput.funput.ime.editing.CompositionRenderMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoundaryCorrectionTest {
    /** VNI `d9u7o7nh2` with the eager restore off: the dead `đườnh` reaches Space. */
    private fun type(fixture: CorrectionFixture, editor: CommittedEditor, word: String = "đườnh") {
        fixture.session.input(editor.proxy, "h")
        fixture.correction.parkAt(word, "đường", "dường")
        fixture.session.input(editor.proxy, " ")
    }

    private fun oneTransaction(mode: CompositionRenderMode) {
        val editor = CommittedEditor()
        val fixture = CorrectionFixture(listOf("đườnh"), mode = mode).apply { correction.chosen = 0 }
        type(fixture, editor)
        assertEquals("đường ", editor.text)
        assertEquals(listOf("choose", "apply(0)"), fixture.correction.calls)
        assertEquals(AppliedCorrection("đườnh", "đườnh", "đường "), fixture.state.applied)
    }

    // KEY_EVENT hosts take the same single replacement through KeyEventBufferWriter, whose
    // strokes need a real KeyEvent; KeyEventDocumentEditorTest covers that writer.
    @Test fun oneTransactionComposing() = oneTransaction(CompositionRenderMode.COMPOSING)
    @Test fun oneTransactionCommitted() = oneTransaction(CompositionRenderMode.COMMITTED)

    @Test
    fun usesReachTheEngine() {
        val fixture = CorrectionFixture(listOf("đườnh")).apply { correction.chosen = 0 }
        type(fixture, CommittedEditor())
        assertEquals(listOf(4, 1), fixture.correction.lastUses?.toList())
    }

    @Test
    fun vetoed() {
        val editor = CommittedEditor()
        val fixture = CorrectionFixture(listOf("đườnh"), dictionary = FakeDictionary(setOf("đườnh")))
        fixture.correction.chosen = 0
        type(fixture, editor)
        assertEquals("đườnh ", editor.text)
        assertEquals(listOf("apply(-1)"), fixture.correction.calls)
        assertEquals(1, fixture.state.declines.recognized)
    }

    @Test
    fun noDictionary() {
        val editor = CommittedEditor()
        val fixture = CorrectionFixture(listOf("đườnh"), dictionary = null).apply { correction.chosen = 0 }
        type(fixture, editor)
        assertEquals("đườnh ", editor.text)
        assertEquals(listOf("apply(-1)"), fixture.correction.calls)
    }

    @Test
    fun autocorrectOff() {
        val editor = CommittedEditor()
        val fixture = CorrectionFixture(listOf("đườnh"), fieldAllows = false).apply { correction.chosen = 0 }
        type(fixture, editor)
        assertEquals("đườnh ", editor.text)
        assertEquals(1, fixture.state.declines.fieldRefused)
    }

    @Test
    fun settingOff() {
        val editor = CommittedEditor()
        val fixture = CorrectionFixture(listOf("đườnh"), enabled = false).apply { correction.chosen = 0 }
        type(fixture, editor)
        assertEquals("đườnh ", editor.text)
        assertEquals(emptyList<String>(), fixture.correction.calls)
    }

    @Test
    fun forced() {
        val editor = CommittedEditor()
        val fixture = CorrectionFixture(listOf("đườnh"), dictionary = FakeDictionary(setOf("đườnh")))
        fixture.state.forced = "dường"
        type(fixture, editor)
        assertEquals("a tap on the bar overrides every gate", "dường ", editor.text)
        assertNull(fixture.state.forced)
    }

    @Test
    fun parksWhenUndecided() {
        val editor = CommittedEditor()
        val fixture = CorrectionFixture(listOf("đườnh")).apply { correction.ambiguous() }
        type(fixture, editor)
        assertEquals("đườnh ", editor.text)
        assertEquals(ParkedCorrection("đườnh", "đườnh ", listOf("đường", "dường")), fixture.state.parked)
        assertEquals("a parked correction is not answered", listOf("choose"), fixture.correction.calls)
    }

    @Test
    fun restoredKeysAreWhatTheBarShows() {
        // The eager restore wrote the raw keys back: the shown word is that, not the syllable.
        val editor = CommittedEditor()
        val fixture = CorrectionFixture(listOf("đườnh"), boundaryOutput = "d9u7o7nh2 ").apply { correction.ambiguous() }
        type(fixture, editor, word = "d9u7o7nh2")
        assertEquals("d9u7o7nh2", fixture.state.parked?.shown)
    }

    @Test
    fun learnsTheRepair() {
        val dictionary = FakeDictionary()
        val fixture = CorrectionFixture(listOf("đườnh"), dictionary = dictionary).apply { correction.chosen = 0 }
        type(fixture, CommittedEditor())
        assertEquals(listOf("đường"), dictionary.learned)
    }
}
