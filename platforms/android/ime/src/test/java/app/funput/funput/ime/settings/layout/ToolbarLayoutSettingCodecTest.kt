package app.funput.funput.ime.settings.layout

import app.funput.funput.keyboard.layout.toolbar.ToolbarSlot
import org.junit.Assert.assertEquals
import org.junit.Test

class ToolbarLayoutSettingCodecTest {
    @Test
    fun `round trip preserves ordered sides`() {
        val prefs = ToolbarLayoutPreferences(
            leading = listOf(ToolbarSlot.EMOJI, ToolbarSlot.MICROPHONE),
            trailing = listOf(ToolbarSlot.PLACEMENT, ToolbarSlot.CLIPBOARD),
        )
        val decoded = ToolbarLayoutSettingCodec.decode(
            ToolbarLayoutSettingCodec.encode(prefs.leading),
            ToolbarLayoutSettingCodec.encode(prefs.trailing),
        )
        assertEquals(prefs, decoded)
    }

    @Test
    fun `missing or garbage values fall back to default`() {
        assertEquals(ToolbarLayoutPreferences.Default, ToolbarLayoutSettingCodec.decode(null, null))
        assertEquals(
            ToolbarLayoutPreferences.Default,
            ToolbarLayoutSettingCodec.decode("NOT_A_SLOT", "EMOJI"),
        )
        assertEquals(
            ToolbarLayoutPreferences.Default,
            ToolbarLayoutSettingCodec.decode("EMOJI", "BOGUS"),
        )
    }
}
