package app.funput.funput.ime.settings

import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardSizingSettingCodecTest {
    @Test
    fun missingOrUnknownSettingFallsBackToNormal() {
        assertEquals(KeyboardSizingProfile.Normal, KeyboardSizingSettingCodec.decode(null))
        assertEquals(KeyboardSizingProfile.Normal, KeyboardSizingSettingCodec.decode("unknown"))
    }

    @Test
    fun allPresetsRoundTrip() {
        KeyboardSizingProfile.Presets.forEach { profile ->
            assertEquals(profile, KeyboardSizingSettingCodec.decode(KeyboardSizingSettingCodec.encode(profile)))
        }
    }
}
