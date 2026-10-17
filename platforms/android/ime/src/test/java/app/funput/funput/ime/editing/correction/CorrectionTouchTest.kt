package app.funput.funput.ime.editing.correction

import app.funput.funput.ime.editing.CommittedEditor
import app.funput.funput.keyboard.touch.KeyTouchAlternate
import app.funput.funput.keyboard.touch.KeyTouchEvidence
import org.junit.Assert.assertEquals
import org.junit.Test

class CorrectionTouchTest {
    private val touch = KeyTouchEvidence('h'.code, 0.6f, listOf(KeyTouchAlternate('g'.code, 0.5f)))

    @Test
    fun evidenceReachesTheEngineBeforeTheKey() {
        val fixture = CorrectionFixture(listOf("h"))
        fixture.session.input(CommittedEditor().proxy, "h", touch)
        assertEquals(listOf(touch), fixture.correction.touches)
        assertEquals(1, fixture.state.spread.count)
    }

    @Test
    fun withoutTouch() {
        val fixture = CorrectionFixture(listOf("h"))
        fixture.session.input(CommittedEditor().proxy, "h")
        assertEquals(emptyList<KeyTouchEvidence>(), fixture.correction.touches)
    }

    @Test
    fun rawCommitDoesNotLeakTheTouch() {
        // Text of more than one scalar bypasses the engine, so it must not leave a touch
        // behind for whatever key comes next.
        val fixture = CorrectionFixture(emptyList())
        fixture.session.input(CommittedEditor().proxy, "ab", touch)
        assertEquals(emptyList<KeyTouchEvidence>(), fixture.correction.touches)
    }

    @Test
    fun settingOffSendsNothing() {
        val fixture = CorrectionFixture(listOf("h"), enabled = false)
        fixture.session.input(CommittedEditor().proxy, "h", touch)
        assertEquals(emptyList<KeyTouchEvidence>(), fixture.correction.touches)
        assertEquals(0, fixture.state.spread.count)
    }
}
