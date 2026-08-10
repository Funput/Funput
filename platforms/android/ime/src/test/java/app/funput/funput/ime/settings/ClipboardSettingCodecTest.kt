package app.funput.funput.ime.settings

import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardSettingCodecTest {
    @Test
    fun `missing and invalid values use privacy first defaults`() {
        assertEquals(ClipboardPreferences.Default, ClipboardSettingCodec.decode(null, null))
        assertEquals(ClipboardExpiry.HOUR, ClipboardSettingCodec.decode(true, "invalid").expiry)
        assertTrue(ClipboardSettingCodec.decode(null, "day").enabled)
    }

    @Test
    fun `enabled value and every expiry round trip`() {
        ClipboardExpiry.entries.forEach { expiry ->
            val decoded = ClipboardSettingCodec.decode(false, ClipboardSettingCodec.encode(expiry))
            assertFalse(decoded.enabled)
            assertEquals(expiry, decoded.expiry)
        }
    }
}
