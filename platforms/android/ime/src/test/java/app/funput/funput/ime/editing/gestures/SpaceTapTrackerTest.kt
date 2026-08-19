package app.funput.funput.ime.editing.gestures

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceTapTrackerTest {
    @Test
    fun twoSpacesInsideTheWindowAreADoubleTap() {
        var now = 0L
        val tracker = SpaceTapTracker(clock = { now })
        val first = tracker.registerSpace()
        now = 200L
        val second = tracker.registerSpace()
        assertFalse(first)
        assertTrue(second)
    }

    @Test
    fun twoSpacesOutsideTheWindowAreTwoSpaces() {
        var now = 0L
        val tracker = SpaceTapTracker(clock = { now })
        tracker.registerSpace()
        now = 400L
        assertFalse(tracker.registerSpace())
    }

    @Test
    fun aFiringTapIsConsumedSoThreeSpacesPunctuateOnce() {
        var now = 0L
        val tracker = SpaceTapTracker(clock = { now })
        tracker.registerSpace()
        now = 100L
        val second = tracker.registerSpace()
        now = 200L
        val third = tracker.registerSpace()
        assertTrue(second)
        assertFalse(third)
    }

    @Test
    fun anotherKeyBetweenTheSpacesBreaksTheSequence() {
        var now = 0L
        val tracker = SpaceTapTracker(clock = { now })
        tracker.registerSpace()
        tracker.reset()
        now = 100L
        assertFalse(tracker.registerSpace())
    }
}
