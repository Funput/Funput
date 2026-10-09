package app.funput.funput.ime.settings.speech

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceInputSettingCodecTest {
    @Test fun freshInstallAllowsVoiceInputByDefault() {
        assertTrue(VoiceInputSettingCodec.decode(null))
    }

    @Test fun explicitPreferenceSurvivesBothDirections() {
        assertFalse(VoiceInputSettingCodec.decode(false))
        assertTrue(VoiceInputSettingCodec.decode(true))
    }
}
