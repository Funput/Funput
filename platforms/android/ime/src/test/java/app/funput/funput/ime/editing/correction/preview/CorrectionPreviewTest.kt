package app.funput.funput.ime.editing.correction.preview

import app.funput.funput.ime.editing.CommittedEditor
import app.funput.funput.ime.editing.correction.CorrectionFixture
import app.funput.funput.ime.editing.correction.FakeDictionary
import app.funput.funput.ime.nativebridge.correction.CorrectionEdit
import app.funput.funput.ime.nativebridge.correction.CorrectionRanking
import app.funput.funput.ime.nativebridge.correction.CorrectionVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CorrectionPreviewTest {
    private val editor = CommittedEditor()

    private fun deadWord(fixture: CorrectionFixture) = fixture.apply {
        session.input(editor.proxy, "h")
        correction.previewCount = 2
        correction.candidates = listOf("đường", "dường")
        correction.ranking = CorrectionRanking(0, 1, CorrectionVerdict.APPLY)
    }

    private fun preview(fixture: CorrectionFixture) = CorrectionPreviewer(fixture.state).compute(fixture.session.composingText)

    @Test
    fun offered() {
        val fixture = deadWord(CorrectionFixture(listOf("đườnh")))
        assertEquals(CorrectionPreview.Offered("đườnh", listOf("đường")), preview(fixture))
    }

    @Test
    fun ambiguous() {
        val fixture = deadWord(CorrectionFixture(listOf("đườnh"))).apply { correction.ambiguous() }
        assertEquals(CorrectionPreview.Offered("đườnh", listOf("đường", "dường")), preview(fixture))
    }

    @Test
    fun liveWord() {
        val fixture = CorrectionFixture(listOf("đươ")).apply { session.input(editor.proxy, "o") }
        assertNull(preview(fixture))
        assertEquals(listOf("preview"), fixture.correction.calls)
    }

    @Test
    fun knownWord() {
        val fixture = deadWord(CorrectionFixture(listOf("đườnh"), dictionary = FakeDictionary(setOf("đườnh"))))
        assertNull(preview(fixture))
        assertEquals("a word the user means is never searched", emptyList<String>(), fixture.correction.calls)
    }

    @Test
    fun fieldRefused() {
        val fixture = deadWord(CorrectionFixture(listOf("đườnh"), fieldAllows = false))
        assertNull(preview(fixture))
    }

    @Test
    fun refusedEverywhere() {
        val fixture = deadWord(CorrectionFixture(listOf("đườnh"))).apply { correction.ranking = CorrectionRanking.Refused }
        assertNull(preview(fixture))
    }

    @Test
    fun appliedShowsTheUndo() {
        val fixture = CorrectionFixture(listOf("đườnh")).apply {
            correction.chosen = 0
            session.input(editor.proxy, "h")
            correction.parkAt("đườnh", "đường")
            session.input(editor.proxy, " ")
        }
        assertEquals(CorrectionPreview.Applied("đườnh"), preview(fixture))
        fixture.correction.undoEdit = null
        assertNull("another key spent the undo", preview(fixture))
    }

    @Test
    fun usesReachThePreview() {
        val fixture = deadWord(CorrectionFixture(listOf("đườnh")))
        preview(fixture)
        assertEquals(listOf(4, 1), fixture.correction.lastUses?.toList())
    }

    @Test
    fun undoFromBar() {
        val fixture = CorrectionFixture(listOf("đườnh")).apply {
            correction.chosen = 0
            session.input(editor.proxy, "h")
            correction.parkAt("đườnh", "đường")
            correction.undoEdit = CorrectionEdit(6, "đườnh ")
            session.input(editor.proxy, " ")
        }
        CorrectionBarActions(fixture.session, { editor.proxy }) {}.undo()
        assertEquals("đườnh ", editor.text)
    }
}
