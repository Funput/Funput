package app.funput.funput.ime.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardFeedbackSettingCodecTest {
    @Test
    fun `defaults to haptics on and sounds off`() {
        assertEquals(
            KeyboardFeedbackPreferences.Default,
            KeyboardFeedbackSettingCodec.decode(null, null),
        )
    }

    @Test
    fun `restores persisted feedback choices independently`() {
        assertEquals(
            KeyboardFeedbackPreferences(hapticsEnabled = false, soundsEnabled = true),
            KeyboardFeedbackSettingCodec.decode(false, true),
        )
    }
}
