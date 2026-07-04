package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.KeyboardHapticType
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyHapticTypeMapperTest {
    @Test
    fun `maps every key family to semantic feedback`() {
        assertMapped(KeyRole.CHARACTER, KeyboardHapticType.KEY_PRESS)
        assertMapped(KeyRole.SPACE, KeyboardHapticType.KEY_PRESS)
        assertMapped(KeyRole.SHIFT, KeyboardHapticType.CONTROL)
        assertMapped(KeyRole.EMOJI, KeyboardHapticType.CONTROL)
        assertMapped(KeyRole.BACKSPACE, KeyboardHapticType.DELETE)
        assertMapped(KeyRole.ENTER, KeyboardHapticType.SUBMIT)
    }

    @Test
    fun `maps suggestions but ignores empty surface`() {
        assertEquals(KeyboardHapticType.CONTROL, KeyHapticTypeMapper.forTarget(null, isSuggestion = true))
        assertNull(KeyHapticTypeMapper.forTarget(null, isSuggestion = false))
    }

    private fun assertMapped(role: KeyRole, expected: KeyboardHapticType) {
        val key = KeySpec(id = role.name, label = role.name, role = role)
        assertEquals(expected, KeyHapticTypeMapper.forTarget(key, isSuggestion = false))
    }
}
