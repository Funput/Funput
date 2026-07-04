package app.funput.funput.keyboard.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardPanelControllerTest {
    @Test
    fun `starts with letters and changes panel once`() {
        val controller = KeyboardPanelController()

        assertEquals(KeyboardPanel.LETTERS, controller.activePanel)
        assertTrue(controller.show(KeyboardPanel.SYMBOLS))
        assertEquals(KeyboardPanel.SYMBOLS, controller.activePanel)
        assertTrue(controller.show(KeyboardPanel.EMOJI))
        assertEquals(KeyboardPanel.EMOJI, controller.activePanel)
        assertFalse(controller.show(KeyboardPanel.EMOJI))
    }
}
