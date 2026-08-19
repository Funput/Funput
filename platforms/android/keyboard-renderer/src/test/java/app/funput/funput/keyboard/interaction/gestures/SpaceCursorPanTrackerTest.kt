package app.funput.funput.keyboard.interaction.gestures

import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceCursorPanTrackerTest {
    private val tracker = SpaceCursorPanTracker()

    @Test
    fun travelBelowOneStepEmitsNothing() {
        assertEquals(0, tracker.update(9f))
    }

    @Test
    fun eachStepEmitsTheDeltaSinceTheLastCall() {
        assertEquals(2, tracker.update(25f))
        assertEquals(1, tracker.update(30f))
    }

    @Test
    fun reversingDirectionEmitsANegativeDelta() {
        tracker.update(30f)

        assertEquals(-2, tracker.update(10f))
    }

    @Test
    fun truncationTowardZeroDoesNotRoundUp() {
        assertEquals(1, tracker.update(19.9f))
        assertEquals(-1, SpaceCursorPanTracker().update(-19.9f))
    }
}
