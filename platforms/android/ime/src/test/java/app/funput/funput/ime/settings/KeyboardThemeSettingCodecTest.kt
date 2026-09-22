package app.funput.funput.ime.settings

import app.funput.funput.theme.KeyboardThemeId
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardThemeSettingCodecTest {
    @Test
    fun missingOrMalformedSettingFallsBackToTheDefaultTheme() {
        assertEquals(KeyboardThemeId.LiquidGlass, KeyboardThemeSettingCodec.decode(null))
        assertEquals(KeyboardThemeId.LiquidGlass, KeyboardThemeSettingCodec.decode("invalid theme"))
    }

    @Test
    fun builtInThemesRoundTrip() {
        listOf(KeyboardThemeId.Dark, KeyboardThemeId.Light).forEach { themeId ->
            assertEquals(themeId, KeyboardThemeSettingCodec.decode(KeyboardThemeSettingCodec.encode(themeId)))
        }
    }

    @Test
    fun futureThemeIdentifierRoundTripsWithoutBeingKnownByCurrentCatalog() {
        val themeId = KeyboardThemeId.of("com.funput.theme.future")

        assertEquals(themeId, KeyboardThemeSettingCodec.decode(KeyboardThemeSettingCodec.encode(themeId)))
    }
}
