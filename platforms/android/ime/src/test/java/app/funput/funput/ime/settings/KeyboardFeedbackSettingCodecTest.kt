package app.funput.funput.ime.settings

import app.funput.funput.keyboard.KeyboardHapticStrength
import app.funput.funput.keyboard.sound.KeyboardSoundChoice
import app.funput.funput.keyboard.sound.KeyboardSoundStyle
import app.funput.funput.keyboard.sound.KeyboardSoundVolume
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

    @Test
    fun `restores the vibration strength, and keeps it while vibration is off`() {
        val off = KeyboardFeedbackSettingCodec.decode(false, false, hapticStrength = "strong")

        assertEquals(KeyboardHapticStrength.STRONG, off.hapticStrength)
        assertEquals(null, off.activeHapticStrength)
        assertEquals(
            KeyboardHapticStrength.STRONG,
            KeyboardFeedbackSettingCodec.decode(true, false, hapticStrength = "strong").activeHapticStrength,
        )
    }

    @Test
    fun `installs from before strengths keep the medium feel`() {
        assertEquals(KeyboardHapticStrength.MEDIUM, KeyboardFeedbackSettingCodec.decode(true, false).hapticStrength)
        assertEquals(
            KeyboardHapticStrength.MEDIUM,
            KeyboardFeedbackSettingCodec.decode(true, false, hapticStrength = "unknown").hapticStrength,
        )
    }

    @Test
    fun `installs from before sound styles keep the system click at its old level`() {
        val upgraded = KeyboardFeedbackSettingCodec.decode(true, true)

        assertEquals(KeyboardSoundChoice(KeyboardSoundStyle.SYSTEM, KeyboardSoundVolume.MEDIUM), upgraded.activeSound)
        val unknown = KeyboardFeedbackSettingCodec.decode(true, true, soundStyle = "piano", soundVolume = "max")
        assertEquals(upgraded.activeSound, unknown.activeSound)
    }

    @Test
    fun `restores the sound style and volume, and keeps them while sound is off`() {
        val off = KeyboardFeedbackSettingCodec.decode(true, false, soundStyle = "marimba", soundVolume = "loud")

        assertEquals(null, off.activeSound)
        assertEquals(KeyboardSoundStyle.MARIMBA, off.soundStyle)
        assertEquals(
            KeyboardSoundChoice(KeyboardSoundStyle.MARIMBA, KeyboardSoundVolume.LOUD),
            KeyboardFeedbackSettingCodec.decode(true, true, soundStyle = "marimba", soundVolume = "loud").activeSound,
        )
    }

    @Test
    fun `key previews stay on until turned off`() {
        assertEquals(true, KeyboardFeedbackSettingCodec.decode(true, false).keyPreviewsEnabled)
        val off = KeyboardFeedbackSettingCodec.decode(true, false, keyPreviewsEnabled = false)
        assertEquals(false, off.keyPreviewsEnabled)
    }
}
