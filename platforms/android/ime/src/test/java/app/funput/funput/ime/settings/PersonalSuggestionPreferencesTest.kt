package app.funput.funput.ime.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalSuggestionPreferencesTest {
    @Test
    fun `defaults enabled without reset request`() {
        val value = PersonalSuggestionPreferences.Default

        assertTrue(value.enabled)
        assertNull(value.resetToken)
        assertNull(value.appliedResetToken)
    }

    @Test
    fun `reset request retries until matching acknowledgment`() {
        val requested = PersonalSuggestionPreferences(true, "request", null)

        assertEquals("request", requested.pendingReset(inFlight = null))
        assertNull(requested.pendingReset(inFlight = "request"))
        assertNull(requested.copy(appliedResetToken = "request").pendingReset(inFlight = null))
    }
}
