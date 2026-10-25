package app.funput.funput.ime.shortcuts

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutWordTrackerTest {
    @Test fun backspaceOnAnEmptyWordDoesNotPretendItEmptiedOne() {
        val tracker = ShortcutWordTracker()
        assertFalse(tracker.backspace())
    }

    @Test fun backspaceThatRemovesTheLastLetterReportsTheWordEnded() {
        val tracker = ShortcutWordTracker()
        tracker.input("v") { false }
        assertTrue(tracker.backspace())
        assertFalse(tracker.isActive)
    }
}