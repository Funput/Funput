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
        assertTrue("typo correction is on for everyone, as on iOS", decoded.typoCorrectionEnabled)
        assertEquals(SmartCompositionPreferences.Default, decoded)
    }

    @Test
    fun `typo correction can be turned off`() {
        assertFalse(SmartCompositionSettingCodec.decode(null, null, null, typoCorrectionEnabled = false).typoCorrectionEnabled)
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
