package app.funput.funput.keyboard.interaction.gestures.cursor

import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceCursorPanTrackerTest {
    private val tracker = SpaceCursorPanTracker()

    @Test
    fun travelBelowOneStepEmitsNothing() {
        assertEquals(CursorPanStep(), tracker.update(9f, 23f))
    }

    @Test
    fun eachStepEmitsTheDeltaSinceTheLastCall() {
        assertEquals(CursorPanStep(columns = 2), tracker.update(25f, 0f))
        assertEquals(CursorPanStep(columns = 1), tracker.update(30f, 0f))
    }

    @Test
    fun reversingDirectionEmitsANegativeDelta() {
        tracker.update(30f, 0f)

        assertEquals(CursorPanStep(columns = -2), tracker.update(10f, 0f))
    }

    @Test
    fun truncationTowardZeroDoesNotRoundUp() {
        assertEquals(CursorPanStep(columns = 1), tracker.update(19.9f, 0f))
        assertEquals(CursorPanStep(columns = -1), SpaceCursorPanTracker().update(-19.9f, 0f))
    }

    @Test
    fun verticalTravelIsMeteredOnItsOwnCoarserStep() {
        assertEquals(CursorPanStep(lines = 2), tracker.update(0f, 48f))
        assertEquals(CursorPanStep(lines = -3), tracker.update(0f, -24f))
    }

    @Test
    fun theTwoAxesAdvanceIndependentlyInOneStep() {
        assertEquals(CursorPanStep(columns = 3, lines = -1), tracker.update(30f, -24f))
    }
}
