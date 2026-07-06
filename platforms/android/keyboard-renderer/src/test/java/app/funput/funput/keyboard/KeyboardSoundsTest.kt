package app.funput.funput.keyboard

import android.media.AudioManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardSoundsTest {
    @Test
    fun `plays only in normal ringer mode`() {
        assertTrue(KeyboardSounds.shouldPlay(AudioManager.RINGER_MODE_NORMAL))
        assertFalse(KeyboardSounds.shouldPlay(AudioManager.RINGER_MODE_VIBRATE))
        assertFalse(KeyboardSounds.shouldPlay(AudioManager.RINGER_MODE_SILENT))
    }

    @Test
    fun `maps semantic keys to Android keyboard sounds`() {
        assertEquals(
            AudioManager.FX_KEYPRESS_STANDARD,
            KeyboardSounds.soundEffect(KeyboardHapticType.KEY_PRESS),
        )
        assertEquals(
            AudioManager.FX_KEYPRESS_SPACEBAR,
            KeyboardSounds.soundEffect(KeyboardHapticType.SPACE),
        )
        assertEquals(
            AudioManager.FX_KEYPRESS_DELETE,
            KeyboardSounds.soundEffect(KeyboardHapticType.DELETE),
        )
        assertEquals(
            AudioManager.FX_KEYPRESS_RETURN,
            KeyboardSounds.soundEffect(KeyboardHapticType.SUBMIT),
        )
    }
}
