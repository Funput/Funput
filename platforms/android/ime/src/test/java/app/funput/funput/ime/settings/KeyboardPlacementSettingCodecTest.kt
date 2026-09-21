package app.funput.funput.ime.settings

import app.funput.funput.keyboard.placement.KeyboardPlacementMode
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardPlacementSettingCodecTest {
    @Test
    fun `missing values use standard mode and default elevation`() {
        assertEquals(
            KeyboardPlacementPreferences.Default,
            KeyboardPlacementSettingCodec.decode(null, null),
        )
    }

    @Test
    fun `known mode and offset decode together`() {
        assertEquals(
            KeyboardPlacementPreferences(KeyboardPlacementMode.ELEVATED, 128f),
            KeyboardPlacementSettingCodec.decode("elevated", 128f),
        )
    }

    @Test
    fun `unknown mode and invalid offsets fall back safely`() {
        assertEquals(
            KeyboardPlacementPreferences.Default,
            KeyboardPlacementSettingCodec.decode("floating", Float.NaN),
        )
        assertEquals(96f, KeyboardPlacementSettingCodec.validOffset(-1f))
    }
}
