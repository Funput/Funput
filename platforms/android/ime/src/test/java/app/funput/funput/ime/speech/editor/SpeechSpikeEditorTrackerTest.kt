package app.funput.funput.ime.speech.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechSpikeEditorTrackerTest {
    private val tracker = SpeechEditorTracker()

    @Test fun unknownRangeOrDisallowedEditorHasNoAnchor() {
        assertNull(tracker.anchor())
        tracker.startInput(true, -1, -1)
        assertNull(tracker.anchor())
        tracker.startInput(true, 2, 5)
        assertNull(tracker.anchor())
        tracker.startInput(false, 2, 2)
        assertNull(tracker.anchor())
    }

    @Test fun unchangedSelectionDoesNotRequireAnotherCallback() {
        tracker.startInput(true, 3, 3)
        val anchor = tracker.anchor()!!
        assertFalse(tracker.selectionChanged(3, 3))
        assertEquals(anchor, tracker.anchor())
        assertTrue(tracker.matches(anchor))
    }

    @Test fun caretMovingAwayAndBackInvalidatesTheOriginalAnchor() {
        tracker.startInput(true, 3, 3)
        val anchor = tracker.anchor()!!
        assertTrue(tracker.selectionChanged(4, 4))
        assertTrue(tracker.selectionChanged(3, 3))
        assertFalse(tracker.matches(anchor))
    }

    @Test fun restartOfIdenticalFieldStillCreatesAnotherGeneration() {
        tracker.startInput(true, 3, 3)
        val anchor = tracker.anchor()!!
        tracker.startInput(true, 3, 3)
        assertNotEquals(anchor.generation, tracker.anchor()!!.generation)
        assertFalse(tracker.matches(anchor))
    }

    @Test fun InputWithoutSelectionChangeInvalidatesRevision() {
        tracker.startInput(true, 3, 3)
        val anchor = tracker.anchor()!!
        tracker.invalidate()
        assertFalse(tracker.matches(anchor))
    }
}
