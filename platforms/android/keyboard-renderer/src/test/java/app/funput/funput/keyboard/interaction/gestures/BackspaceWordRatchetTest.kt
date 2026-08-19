package app.funput.funput.keyboard.interaction.gestures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackspaceWordRatchetTest {
    private val ratchet = BackspaceWordRatchet()

    @Test
    fun claimsALeftwardHorizontalRubInsideTheKeycap() {
        assertTrue(ratchet.shouldClaim(translationX = -16f, translationY = 0f))
        assertFalse(ratchet.shouldClaim(translationX = -15f, translationY = 0f))
        assertFalse(ratchet.shouldClaim(translationX = -20f, translationY = 20f))
        assertFalse(ratchet.shouldClaim(translationX = 16f, translationY = 0f))
    }

    @Test
    fun oneStepDeletesOneWord() {
        assertEquals(1, ratchet.update(-40f))
        assertTrue(ratchet.hasDeleted)
    }

    @Test
    fun furtherTravelRatchetsWithoutRewinding() {
        assertEquals(1, ratchet.update(-45f))
        assertEquals(1, ratchet.update(-80f))
        assertEquals(0, ratchet.update(-50f))
        assertEquals(0, ratchet.update(20f))
    }

    @Test
    fun travelShortOfAStepHasNotDeleted() {
        assertEquals(0, ratchet.update(-16f))
        assertFalse(ratchet.hasDeleted)
    }
}
