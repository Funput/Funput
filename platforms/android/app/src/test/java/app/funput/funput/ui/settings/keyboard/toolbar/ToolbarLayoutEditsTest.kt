package app.funput.funput.ui.settings.keyboard.toolbar

import app.funput.funput.ime.settings.layout.ToolbarLayoutPreferences
import app.funput.funput.keyboard.layout.toolbar.ToolbarSlot
import org.junit.Assert.assertEquals
import org.junit.Test

class ToolbarLayoutEditsTest {
    private val prefs = ToolbarLayoutPreferences(
        leading = listOf(ToolbarSlot.MICROPHONE, ToolbarSlot.PLACEMENT),
        trailing = listOf(ToolbarSlot.EMOJI, ToolbarSlot.CLIPBOARD),
    )

    @Test
    fun `move up and down reorder within a side`() {
        assertEquals(
            listOf(ToolbarSlot.PLACEMENT, ToolbarSlot.MICROPHONE),
            ToolbarLayoutEdits.moveDown(prefs, ToolbarSide.LEADING, 0).leading,
        )
        assertEquals(
            listOf(ToolbarSlot.CLIPBOARD, ToolbarSlot.EMOJI),
            ToolbarLayoutEdits.moveUp(prefs, ToolbarSide.TRAILING, 1).trailing,
        )
    }

    @Test
    fun `move to other side appends onto the destination`() {
        val moved = ToolbarLayoutEdits.moveToOtherSide(prefs, ToolbarSide.LEADING, 0)
        assertEquals(listOf(ToolbarSlot.PLACEMENT), moved.leading)
        assertEquals(
            listOf(ToolbarSlot.EMOJI, ToolbarSlot.CLIPBOARD, ToolbarSlot.MICROPHONE),
            moved.trailing,
        )
    }

    @Test
    fun `boundary moves are no-ops`() {
        assertEquals(prefs, ToolbarLayoutEdits.moveUp(prefs, ToolbarSide.LEADING, 0))
        assertEquals(prefs, ToolbarLayoutEdits.moveDown(prefs, ToolbarSide.TRAILING, 1))
    }
}
