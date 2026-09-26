package app.funput.funput.keyboard.layout.panels

import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeyboardInputMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelSearchKeyboardLayoutsTest {
    @Test fun `telex keeps four rows with its key hints`() {
        val layout = PanelSearchKeyboardLayouts.letters(KeyboardInputMethod.TELEX, "Tìm emoji")
        assertEquals(4, layout.rows.size)
        assertNull(layout.suggestionBar)
        assertEquals("qwertyuiop", layout.rows[0].keys.joinToString("") { it.label })
        assertNotNull(layout.rows[0].keys.single { it.label == "r" }.secondaryLabel)
        assertEquals(listOf(KeyRole.EMOJI, KeyRole.SPACE, KeyRole.ENTER), layout.rows[3].keys.map { it.role })
    }

    @Test fun `vni adds its modifier digit row`() {
        val layout = PanelSearchKeyboardLayouts.letters(KeyboardInputMethod.VNI, "Tìm emoji")
        assertEquals(5, layout.rows.size)
        assertTrue(layout.rows[0].keys.all { it.role == KeyRole.VNI_MODIFIER })
        assertTrue(layout.rows[1].keys.all { it.secondaryLabel == null })
    }

    @Test fun `a literal field gets plain letters`() {
        val layout = PanelSearchKeyboardLayouts.letters(null, "Tìm emoji")
        assertEquals(4, layout.rows.size)
        assertTrue(layout.rows.flatMap { it.keys }.all { it.secondaryLabel == null })
    }

    @Test fun `bottom row keeps the search actions`() {
        val keys = PanelSearchKeyboardLayouts.letters(KeyboardInputMethod.TELEX, "Tìm emoji").rows.flatMap { it.keys }
        assertNotNull(keys.singleOrNull { it.role == KeyRole.SHIFT })
        assertNotNull(keys.singleOrNull { it.role == KeyRole.BACKSPACE })
        assertEquals("Tìm emoji", keys.single { it.role == KeyRole.SPACE }.spaceLabelOverride)
        assertEquals("Xong", keys.single { it.role == KeyRole.ENTER }.label)
    }

    @Test fun `each method gets its own page identity`() {
        val ids = (KeyboardInputMethod.entries + null).map { PanelSearchKeyboardLayouts.letters(it, "Tìm").id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
