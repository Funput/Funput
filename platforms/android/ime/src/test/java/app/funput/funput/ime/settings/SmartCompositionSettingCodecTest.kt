package app.funput.funput.ime.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartCompositionSettingCodecTest {
    @Test
    fun `missing keys fall back to defaults`() {
        val decoded = SmartCompositionSettingCodec.decode(null, null)

        assertFalse(decoded.spellCheckEnabled)
        assertTrue(decoded.smartRestoreEnabled)
        assertEquals(SmartCompositionPreferences.Default, decoded)
    }

    @Test
    fun `both flags round trip`() {
        val expected = SmartCompositionPreferences(
            spellCheckEnabled = true,
            smartRestoreEnabled = false,
        )

        assertEquals(
            expected,
            SmartCompositionSettingCodec.decode(
                spellCheckEnabled = expected.spellCheckEnabled,
                smartRestoreEnabled = expected.smartRestoreEnabled,
            ),
        )
    }
}
