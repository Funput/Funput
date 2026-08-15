package app.funput.funput.ime.settings

import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardSizingSettingCodecTest {
    @Test
    fun missingOrUnreadableSettingFallsBackToDefault() {
        assertEquals(KeyboardSizingProfile.Default, KeyboardSizingSettingCodec.decode(null))
        assertEquals(KeyboardSizingProfile.Default, KeyboardSizingSettingCodec.decode("unknown"))
        assertEquals(KeyboardSizingProfile.Default, KeyboardSizingSettingCodec.decode("NaN"))
    }

    @Test
    fun scalesRoundTrip() {
        listOf(0.85f, 1f, 1.08f, 1.2f).forEach { scale ->
            val profile = KeyboardSizingProfile.scaled(scale)
            assertEquals(profile, KeyboardSizingSettingCodec.decode(KeyboardSizingSettingCodec.encode(profile)))
        }
    }

    @Test
    fun outOfRangeScalesDecodeClamped() {
        assertEquals(
            KeyboardSizingProfile.scaled(KeyboardSizingProfile.MaxScale),
            KeyboardSizingSettingCodec.decode("3.0"),
        )
    }

    @Test
    fun legacyPresetIdsKeepTheirSize() {
        assertEquals(KeyboardSizingProfile.scaled(0.92f), KeyboardSizingSettingCodec.decode("compact"))
        assertEquals(KeyboardSizingProfile.scaled(1f), KeyboardSizingSettingCodec.decode("normal"))
        assertEquals(KeyboardSizingProfile.scaled(1.08f), KeyboardSizingSettingCodec.decode("large"))
    }
}
