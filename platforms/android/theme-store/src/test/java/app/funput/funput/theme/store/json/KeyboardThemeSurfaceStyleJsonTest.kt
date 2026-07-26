package app.funput.funput.theme.store.json

import app.funput.funput.theme.KeyboardKeySurfaceStyle
import app.funput.funput.theme.KeyboardThemes
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardThemeSurfaceStyleJsonTest {
    @Test
    fun unknownKeySurfaceStyleFallsBackToFlat() {
        val json = JSONObject(KeyboardThemeJson.encode(KeyboardThemes.Ink))
        json.put("keySurfaceStyle", "HOLOGRAPHIC")

        assertEquals(
            KeyboardKeySurfaceStyle.FLAT,
            KeyboardThemeJson.decode(json.toString()).keySurfaceStyle,
        )
    }

    @Test
    fun glassKeySurfaceStyleRoundTrips() {
        listOf(KeyboardThemes.GlassDark, KeyboardThemes.GlassLight).forEach { theme ->
            assertEquals(theme, KeyboardThemeJson.decode(KeyboardThemeJson.encode(theme)))
        }
    }

    @Test
    fun bareThemeTextRoundTripsThroughThePublicHelper() {
        assertEquals(
            KeyboardThemes.Paper,
            KeyboardThemeJson.decode(KeyboardThemeJson.encode(KeyboardThemes.Paper)),
        )
    }
}
