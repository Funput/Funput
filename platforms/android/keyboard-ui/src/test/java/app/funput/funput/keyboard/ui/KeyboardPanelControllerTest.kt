package app.funput.funput.keyboard.ui

import app.funput.funput.keyboard.model.KeyboardLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
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
        assertTrue(controller.show(KeyboardPanel.LETTERS))
        assertFalse(controller.show(KeyboardPanel.LETTERS))
    }

    @Test
    fun `symbols accepts both symbols layouts and remains the logical panel`() {
        val controller = KeyboardPanelController()

        controller.showSymbols(KeyboardLayoutMode.SYMBOLS_PRIMARY)
        assertEquals(KeyboardPanel.SYMBOLS, controller.activePanel)
        controller.showSymbols(KeyboardLayoutMode.SYMBOLS_SECONDARY)

        assertEquals(KeyboardPanel.SYMBOLS, controller.activePanel)
    }

    @Test
    fun `symbols rejects letters layout`() {
        val controller = KeyboardPanelController()

        assertThrows(IllegalArgumentException::class.java) {
            controller.showSymbols(KeyboardLayoutMode.LETTERS)
        }
        assertEquals(KeyboardPanel.LETTERS, controller.activePanel)
    }
}
