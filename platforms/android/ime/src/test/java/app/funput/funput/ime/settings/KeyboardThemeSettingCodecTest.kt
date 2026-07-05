package app.funput.funput.ime.settings

import app.funput.funput.theme.KeyboardThemeId
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardThemeSettingCodecTest {
    @Test
    fun missingOrUnknownSettingFallsBackToDark() {
        assertEquals(KeyboardThemeId.DARK, KeyboardThemeSettingCodec.decode(null))
        assertEquals(KeyboardThemeId.DARK, KeyboardThemeSettingCodec.decode("unknown"))
    }

    @Test
    fun allPresetsRoundTrip() {
        KeyboardThemeId.Presets.forEach { themeId ->
            assertEquals(themeId, KeyboardThemeSettingCodec.decode(KeyboardThemeSettingCodec.encode(themeId)))
        }
    }
}
