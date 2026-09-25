package app.funput.funput.ime.hardware.visibility

import app.funput.funput.ime.settings.hardware.HardwareKeyboardPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftKeyboardVisibilityStateTest {
    @Test
    fun `defaults follow Android and keep the hotkey on`() {
        val state = SoftKeyboardVisibilityState()

        assertFalse(state.showsWithHardwareKeyboard)
        assertTrue(state.hotkeyEnabled)
    }

    @Test
    fun `the setting alone decides until the hotkey is used`() {
        val state = SoftKeyboardVisibilityState()

        state.update(preferences(showsSoftKeyboard = true))
        assertTrue(state.showsWithHardwareKeyboard)

        state.update(preferences(showsSoftKeyboard = false))
        assertFalse(state.showsWithHardwareKeyboard)
    }

    @Test
    fun `the hotkey flips what is on screen, whatever the setting says`() {
        val state = SoftKeyboardVisibilityState()

        assertEquals(SoftKeyboardRequest.SHOW, state.toggle(currentlyShown = false))
        assertTrue(state.showsWithHardwareKeyboard)

        assertEquals(SoftKeyboardRequest.HIDE, state.toggle(currentlyShown = true))
        assertFalse(state.showsWithHardwareKeyboard)
    }

    @Test
    fun `hiding with the hotkey outlasts a setting that shows the keyboard`() {
        val state = SoftKeyboardVisibilityState()
        state.update(preferences(showsSoftKeyboard = true))

        state.toggle(currentlyShown = true)

        assertFalse(state.showsWithHardwareKeyboard)
    }

    @Test
    fun `detaching the keyboard drops the hotkey override`() {
        val state = SoftKeyboardVisibilityState()
        state.toggle(currentlyShown = false)

        state.onKeyboardDetached()

        assertFalse(state.showsWithHardwareKeyboard)
    }

    @Test
    fun `changing the setting drops the hotkey override`() {
        val state = SoftKeyboardVisibilityState()
        state.toggle(currentlyShown = true)

        state.update(preferences(showsSoftKeyboard = true))

        assertTrue(state.showsWithHardwareKeyboard)
    }

    @Test
    fun `re-emitting the same setting keeps the hotkey override`() {
        val state = SoftKeyboardVisibilityState()
        state.toggle(currentlyShown = false)

        state.update(HardwareKeyboardPreferences.Default)

        assertTrue(state.showsWithHardwareKeyboard)
    }

    @Test
    fun `turning the hotkey off is reported`() {
        val state = SoftKeyboardVisibilityState()

        state.update(preferences(toggleHotkeyEnabled = false))

        assertFalse(state.hotkeyEnabled)
    }

    private fun preferences(
        showsSoftKeyboard: Boolean = HardwareKeyboardPreferences.Default.showsSoftKeyboard,
        toggleHotkeyEnabled: Boolean = HardwareKeyboardPreferences.Default.toggleHotkeyEnabled,
    ) = HardwareKeyboardPreferences(showsSoftKeyboard, toggleHotkeyEnabled)
}
