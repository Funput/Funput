package app.funput.funput.ime.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberRowSettingCodecTest {
    @Test
    fun `missing value falls back to compact Telex default`() {
        assertFalse(NumberRowSettingCodec.decode(null))
        assertEquals(NumberRowSettings.DefaultShowsNumberRow, NumberRowSettingCodec.decode(null))
    }

    @Test
    fun `explicit values round trip`() {
        assertTrue(NumberRowSettingCodec.decode(true))
        assertFalse(NumberRowSettingCodec.decode(false))
    }
}
