package app.funput.funput.ime.shortcuts

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutWordTrackerTest {
    @Test fun anEmptyTrackerStillReportsTheWordAsEnded() {
        // A reopened Vietnamese word never lived in this tracker. Deleting it has to
        // read as "the word is over" so a library that arrived mid-word can install.
        val tracker = ShortcutWordTracker()
        assertTrue(tracker.backspace())
    }

    @Test fun backspaceThatRemovesTheLastLetterReportsTheWordEnded() {
        val tracker = ShortcutWordTracker()
        tracker.input("v") { false }
        assertTrue(tracker.backspace())
        assertFalse(tracker.isActive)
    }
}