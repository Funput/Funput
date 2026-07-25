package app.funput.funput.theme.store.json

import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
import app.funput.funput.theme.KeyboardThemes
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class KeyboardThemeJsonCodecTest {
    @Test
    fun encodeThenDecodeRoundTripsEveryField() {
        val descriptor = descriptor(
            baseThemeId = KeyboardThemeId.Light,
            backgroundImage = KeyboardThemeBackgroundImage("content://image/ocean", 0.42f),
        )

        val decoded = KeyboardThemeJsonCodec.decode(KeyboardThemeJsonCodec.encode(descriptor))

        assertEquals(descriptor, decoded)
    }

    @Test
    fun encodeOmitsAbsentOptionalFields() {
        val json = JSONObject(KeyboardThemeJsonCodec.encode(descriptor()))

        assertEquals(KeyboardThemeJsonCodec.SchemaVersion, json.getInt("schemaVersion"))
        assertEquals(false, json.has("baseThemeId"))
        assertEquals(false, json.has("backgroundImage"))
    }

    @Test
    fun decodeToleratesAThemeMissingTokensAddedAfterItWasSaved() {
        val json = JSONObject(KeyboardThemeJsonCodec.encode(descriptor()))
        val theme = json.getJSONObject("theme")
        listOf(
            "specialLabelColor",
            "accentKeyColor",
            "accentLabelColor",
            "popupSurfaceColor",
            "suggestionHighlightColor",
            "pressedKeyScale",
        ).forEach(theme::remove)
        json.remove("schemaVersion")

        val decoded = KeyboardThemeJsonCodec.decode(json.toString()).theme

        // Each absent token falls back to the value KeyboardTheme itself derives it from.
        assertEquals(decoded.labelColor, decoded.specialLabelColor)
        assertEquals(decoded.specialKeyColor, decoded.accentKeyColor)
        assertEquals(decoded.accentColor, decoded.accentLabelColor)
        assertEquals(decoded.keyColor, decoded.popupSurfaceColor)
        assertEquals(decoded.labelColor, decoded.suggestionHighlightColor)
        assertEquals(1f, decoded.pressedKeyScale, 0f)
    }

    @Test
    fun decodeIgnoresTokensWrittenByANewerBuild() {
        val json = JSONObject(KeyboardThemeJsonCodec.encode(descriptor()))
        json.getJSONObject("theme").put("keycapInsetDp", 3.5)
        json.put("schemaVersion", KeyboardThemeJsonCodec.SchemaVersion + 1)

        assertEquals(KeyboardThemes.Ink, KeyboardThemeJsonCodec.decode(json.toString()).theme)
    }

    @Test
    fun decodeFailsOnACorruptFileRatherThanInventingATheme() {
        val json = JSONObject(KeyboardThemeJsonCodec.encode(descriptor()))
        json.getJSONObject("theme").remove("labelColor")

        assertThrows(Exception::class.java) { KeyboardThemeJsonCodec.decode(json.toString()) }
    }

    @Test
    fun decodeReadsAnAbsentBaseThemeIdAsNull() {
        val decoded = KeyboardThemeJsonCodec.decode(KeyboardThemeJsonCodec.encode(descriptor()))

        assertNull(decoded.baseThemeId)
        assertNull(decoded.backgroundImage)
    }

    @Test
    fun bareThemeTextRoundTripsThroughThePublicHelper() {
        assertEquals(
            KeyboardThemes.Paper,
            KeyboardThemeJson.decode(KeyboardThemeJson.encode(KeyboardThemes.Paper)),
        )
    }

    private fun descriptor(
        baseThemeId: KeyboardThemeId? = null,
        backgroundImage: KeyboardThemeBackgroundImage? = null,
    ) = KeyboardThemeDescriptor(
        id = KeyboardThemeId.of("custom.ocean"),
        version = 3,
        name = "Ocean",
        author = "Me",
        origin = KeyboardThemeOrigin.CUSTOM,
        baseThemeId = baseThemeId,
        backgroundImage = backgroundImage,
        theme = KeyboardThemes.Ink,
    )
}
