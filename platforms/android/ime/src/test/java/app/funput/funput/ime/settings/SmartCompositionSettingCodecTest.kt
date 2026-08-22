package app.funput.funput.ime.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartCompositionSettingCodecTest {
    @Test
    fun `missing keys fall back to defaults`() {
        val decoded = SmartCompositionSettingCodec.decode(null, null, null)

        assertFalse(decoded.spellCheckEnabled)
        assertTrue(decoded.smartRestoreEnabled)
        assertTrue(decoded.autoCapitalizeEnabled)
        assertEquals(SmartCompositionPreferences.Default, decoded)
    }

    @Test
    fun `flags round trip including a disabled auto-capitalize`() {
        val expected = SmartCompositionPreferences(
            spellCheckEnabled = true,
            smartRestoreEnabled = false,
            autoCapitalizeEnabled = false,
        )

        assertEquals(
            expected,
            SmartCompositionSettingCodec.decode(
                spellCheckEnabled = expected.spellCheckEnabled,
                smartRestoreEnabled = expected.smartRestoreEnabled,
                autoCapitalizeEnabled = expected.autoCapitalizeEnabled,
            ),
        )
    }
}
