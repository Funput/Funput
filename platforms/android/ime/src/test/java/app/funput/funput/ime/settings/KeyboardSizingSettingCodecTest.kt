package app.funput.funput.ime.settings

import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardSizingSettingCodecTest {
    @Test
    fun missingOrUnknownSettingFallsBackToDefault() {
        assertEquals(KeyboardSizingProfile.Default, KeyboardSizingSettingCodec.decode(null))
        assertEquals(KeyboardSizingProfile.Default, KeyboardSizingSettingCodec.decode("unknown"))
    }

    @Test
    fun allPresetsRoundTrip() {
        KeyboardSizingProfile.Presets.forEach { profile ->
            assertEquals(profile, KeyboardSizingSettingCodec.decode(KeyboardSizingSettingCodec.encode(profile)))
        }
    }
}
