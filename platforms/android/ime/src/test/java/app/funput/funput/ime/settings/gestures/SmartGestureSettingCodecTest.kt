package app.funput.funput.ime.settings.gestures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartGestureSettingCodecTest {
    @Test
    fun `missing value defaults to on`() {
        assertTrue(SmartGestureSettingCodec.decode(null))
        assertEquals(SmartGestureSettings.DefaultEnabled, SmartGestureSettingCodec.decode(null))
    }

    @Test
    fun `explicit values round trip`() {
        assertTrue(SmartGestureSettingCodec.decode(true))
        assertFalse(SmartGestureSettingCodec.decode(false))
    }
}
