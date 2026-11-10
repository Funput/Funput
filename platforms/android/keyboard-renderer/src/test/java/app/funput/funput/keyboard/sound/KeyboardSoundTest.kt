package app.funput.funput.keyboard.sound

import app.funput.funput.keyboard.KeyboardHapticType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyboardSoundTest {
    @Test
    fun `every press maps to a key, delete or modifier sound`() {
        val kinds = KeyboardHapticType.entries.associateWith(KeyboardSoundKind::of)

        assertEquals(KeyboardSoundKind.KEY, kinds[KeyboardHapticType.KEY_PRESS])
        assertEquals(KeyboardSoundKind.DELETE, kinds[KeyboardHapticType.DELETE])
        assertEquals(KeyboardSoundKind.DELETE, kinds[KeyboardHapticType.DELETE_REPEAT])
        assertEquals(KeyboardSoundKind.MODIFIER, kinds[KeyboardHapticType.SPACE])
        assertEquals(KeyboardSoundKind.MODIFIER, kinds[KeyboardHapticType.CONTROL])
        assertEquals(KeyboardSoundKind.MODIFIER, kinds[KeyboardHapticType.SUBMIT])
    }

    @Test
    fun `styles and volumes read back what they store, and fall back to the defaults`() {
        KeyboardSoundStyle.entries.forEach { assertEquals(it, KeyboardSoundStyle.fromStorage(it.storageId)) }
        KeyboardSoundVolume.entries.forEach { assertEquals(it, KeyboardSoundVolume.fromStorage(it.storageId)) }
        assertEquals(KeyboardSoundStyle.SYSTEM, KeyboardSoundStyle.fromStorage("piano"))
        assertEquals(KeyboardSoundVolume.MEDIUM, KeyboardSoundVolume.fromStorage(null))
    }

    @Test
    fun `medium keeps the level Funput clicked at before volumes, and the levels rise`() {
        val default = KeyboardSoundChoice.Default
        assertEquals(KeyboardSoundChoice(KeyboardSoundStyle.SYSTEM, KeyboardSoundVolume.MEDIUM), default)
        assertEquals(0.5f, KeyboardSoundVolume.MEDIUM.gain)
        val gains = KeyboardSoundVolume.entries.map { it.gain }
        assertEquals(gains.sorted(), gains)
        assertEquals(1f, gains.last())
    }

    @Test
    fun `every bundled style has its own file for each kind, and the system style has none`() {
        KeyboardSoundKind.entries.forEach { kind -> assertNull(KeyboardSoundStyle.SYSTEM.rawResource(kind)) }
        val bundled = KeyboardSoundStyle.entries.filter { it != KeyboardSoundStyle.SYSTEM }
        val files = bundled.flatMap { style -> KeyboardSoundKind.entries.map(style::rawResource) }
        assertEquals(bundled.size * KeyboardSoundKind.entries.size, files.filterNotNull().distinct().size)
    }

    @Test
    fun `a press on a sample still decoding plays once it decodes, and a style switch drops it`() {
        val readiness = KeyboardSoundPool.Readiness()

        assertNull(readiness.press(id = 1, gain = 0.3f))
        assertNull(readiness.decoded(2))
        assertEquals(0.3f, readiness.decoded(1))
        assertEquals(1f, readiness.press(id = 1, gain = 1f))
        assertNull(readiness.decoded(1))

        assertNull(readiness.press(id = 3, gain = 0.5f))
        readiness.reset()
        assertNull(readiness.decoded(3))
        assertNull(readiness.press(id = 1, gain = 0.5f))
    }
}
