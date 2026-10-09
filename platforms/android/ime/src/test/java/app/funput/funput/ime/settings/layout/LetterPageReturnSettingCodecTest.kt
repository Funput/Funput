package app.funput.funput.ime.settings.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LetterPageReturnSettingCodecTest {
    @Test
    fun `missing value defaults to on for new and upgrading installs`() {
        assertTrue(LetterPageReturnSettingCodec.decode(null))
        assertEquals(LetterPageReturnSettings.DefaultEnabled, LetterPageReturnSettingCodec.decode(null))
    }

    @Test
    fun `explicit values round trip`() {
        assertTrue(LetterPageReturnSettingCodec.decode(true))
        assertFalse(LetterPageReturnSettingCodec.decode(false))
    }
}
