package app.funput.funput.ime.editing.correction.preview

import app.funput.funput.ime.editing.CommittedEditor
import app.funput.funput.ime.editing.correction.CorrectionFixture
import app.funput.funput.ime.editing.correction.FakeDictionary
import app.funput.funput.ime.editing.correction.ParkedCorrection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectionBarActionsTest {
    private val editor = CommittedEditor()
    private val dictionary = FakeDictionary()

    private fun fixture() = CorrectionFixture(listOf("đườnh", "x"), dictionary = dictionary).apply {
        session.input(editor.proxy, "h")
    }

    private fun parked() = fixture().apply {
        correction.ambiguous()
        correction.parkAt("đườnh", "đường", "dường")
        session.input(editor.proxy, " ")
    }

    private fun actions(fixture: CorrectionFixture) =
        CorrectionBarActions(fixture.session, { editor.proxy }) { fixture.session.input(editor.proxy, " ") }

    @Test
    fun accept() {
        // Before Space: the tap is a Space that applies the tapped candidate.
        val fixture = fixture().apply { correction.parkAt("đườnh", "đường", "dường") }
        assertTrue(actions(fixture).accept("dường"))
        assertEquals("dường ", editor.text)
        assertNull(fixture.state.forced)
    }

    @Test
    fun refuse() {
        val fixture = fixture()
        assertTrue(actions(fixture).refuse("đườnh"))
        assertEquals(listOf("refuse"), fixture.correction.calls)
        assertEquals(listOf("đườnh"), dictionary.kept)
        assertEquals(1, fixture.state.declines.refused)
    }

    @Test
    fun refuseNeedsTheWordStillThere() {
        val fixture = fixture()
        assertFalse(actions(fixture).refuse("khác"))
    }

    @Test
    fun parkedAfterSpace() {
        val fixture = parked()
        assertEquals("đườnh ", editor.text)
        assertEquals(ParkedCorrection("đườnh", "đườnh ", listOf("đường", "dường")), fixture.state.parked)
    }

    @Test
    fun acceptParked() {
        val fixture = parked()
        assertTrue(actions(fixture).accept("dường"))
        assertEquals("dường ", editor.text)
        assertEquals("the swap arms an undo like Space does", "dường ", fixture.state.applied?.corrected)
    }

    @Test
    fun acceptParkedNeedsTheDocument() {
        val fixture = parked()
        editor.textBeforeCursor = "khác "
        assertFalse(actions(fixture).accept("dường"))
        assertEquals("apply(-1)", fixture.correction.calls.last())
        assertEquals("đườnh ", editor.text)
    }

    @Test
    fun refuseParked() {
        val fixture = parked()
        assertTrue(actions(fixture).refuse("đườnh"))
        assertNull(fixture.state.parked)
        assertEquals(listOf("đườnh"), dictionary.kept)
    }

    @Test
    fun nextKeyFlushes() {
        val fixture = parked()
        fixture.session.input(editor.proxy, "x")
        assertNull(fixture.state.parked)
    }
}
