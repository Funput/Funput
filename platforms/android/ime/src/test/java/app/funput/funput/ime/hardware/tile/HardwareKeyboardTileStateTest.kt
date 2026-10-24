package app.funput.funput.ime.hardware.tile

import android.service.quicksettings.Tile
import app.funput.funput.ime.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareKeyboardTileStateTest {
    @Test
    fun `when soft keyboard is hidden beside hardware keyboard the tile is inactive`() {
        val state = HardwareKeyboardTileState.from(showsSoftKeyboard = false)

        assertFalse(state.showsSoftKeyboard)
        assertEquals(Tile.STATE_INACTIVE, state.tileState)
        assertEquals(R.string.hardware_keyboard_tile_label, state.labelRes)
        assertEquals(R.string.hardware_keyboard_tile_subtitle_hidden, state.subtitleRes)
        assertEquals(R.string.hardware_keyboard_tile_description_hidden, state.contentDescriptionRes)
    }

    @Test
    fun `when soft keyboard is shown beside hardware keyboard the tile is active`() {
        val state = HardwareKeyboardTileState.from(showsSoftKeyboard = true)

        assertTrue(state.showsSoftKeyboard)
        assertEquals(Tile.STATE_ACTIVE, state.tileState)
        assertEquals(R.string.hardware_keyboard_tile_label, state.labelRes)
        assertEquals(R.string.hardware_keyboard_tile_subtitle_shown, state.subtitleRes)
        assertEquals(R.string.hardware_keyboard_tile_description_shown, state.contentDescriptionRes)
    }

    @Test
    fun `toggling flips between active and inactive states`() {
        val inactive = HardwareKeyboardTileState.from(showsSoftKeyboard = false)

        val active = inactive.toggled()
        assertTrue(active.showsSoftKeyboard)
        assertEquals(Tile.STATE_ACTIVE, active.tileState)

        val backToInactive = active.toggled()
        assertEquals(inactive, backToInactive)
    }
}
