package app.funput.funput.theme.store.json

import app.funput.funput.theme.KeyboardLabelWeight
import app.funput.funput.theme.KeyboardThemes
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** The key label size and weight a custom theme can set, as stored on disk. */
class KeyboardThemeLabelJsonTest {
    @Test
    fun labelSizeAndWeightRoundTrip() {
        val theme = KeyboardThemes.Ink.copy(labelScale = 1.15f, labelWeight = KeyboardLabelWeight.BOLD)

        assertEquals(theme, KeyboardThemeJson.decode(KeyboardThemeJson.encode(theme)))
    }

    @Test
    fun aThemeSavedBeforeTheTokensExistedKeepsItsLabels() {
        val json = JSONObject(KeyboardThemeJson.encode(KeyboardThemes.Paper))
        json.remove("labelScale")
        json.remove("labelWeight")

        val decoded = KeyboardThemeJson.decode(json.toString())

        assertEquals(1f, decoded.labelScale)
        assertEquals(KeyboardLabelWeight.REGULAR, decoded.labelWeight)
    }

    @Test
    fun anOutOfRangeSizeIsClampedInsteadOfCrashingTheKeyboard() {
        val json = JSONObject(KeyboardThemeJson.encode(KeyboardThemes.Ink))
        json.put("labelScale", 4.0)

        assertEquals(1.3f, KeyboardThemeJson.decode(json.toString()).labelScale)
    }

    @Test
    fun anUnknownWeightFallsBackToRegular() {
        val json = JSONObject(KeyboardThemeJson.encode(KeyboardThemes.Ink))
        json.put("labelWeight", "BLACK")

        assertEquals(KeyboardLabelWeight.REGULAR, KeyboardThemeJson.decode(json.toString()).labelWeight)
    }
}
