package app.funput.funput.keyboard.layout.toolbar

import org.junit.Assert.assertEquals
import org.junit.Test

class ToolbarArrangementTest {
    @Test
    fun `default keeps mic and placement leading with emoji outermost trailing`() {
        assertEquals(
            listOf(ToolbarSlot.MICROPHONE, ToolbarSlot.PLACEMENT),
            ToolbarArrangement.Default.leading,
        )
        assertEquals(
            listOf(ToolbarSlot.CLIPBOARD, ToolbarSlot.EMOJI),
            ToolbarArrangement.Default.trailing,
        )
    }

    @Test
    fun `normalize drops unavailable duplicates and restores missing onto default sides`() {
        val raw = ToolbarArrangement(
            leading = listOf(ToolbarSlot.EMOJI, ToolbarSlot.EMOJI, ToolbarSlot.CLIPBOARD),
            trailing = listOf(ToolbarSlot.MICROPHONE),
        )
        val normalized = raw.normalize(
            setOf(ToolbarSlot.EMOJI, ToolbarSlot.MICROPHONE, ToolbarSlot.PLACEMENT),
        )
        assertEquals(listOf(ToolbarSlot.EMOJI, ToolbarSlot.PLACEMENT), normalized.leading)
        assertEquals(listOf(ToolbarSlot.MICROPHONE), normalized.trailing)
    }
}
