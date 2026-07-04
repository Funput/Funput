package app.funput.funput.keyboard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyActionMapperTest {
    @Test
    fun textProducingKeysIncludeTheirIdentityAndLabel() {
        val textRoles = listOf(
            KeyRole.CHARACTER,
            KeyRole.VNI_MODIFIER,
            KeyRole.PUNCTUATION,
        )

        textRoles.forEach { role ->
            val key = key(role, label = "x")
            assertEquals(
                KeyAction.Input(keyId = key.id, text = "x"),
                key.toKeyAction(ShiftState.OFF),
            )
        }
    }

    @Test
    fun commandKeysMapToSemanticActions() {
        val expectedActions = mapOf(
            KeyRole.SHIFT to KeyAction.Shift(ShiftState.OFF),
            KeyRole.BACKSPACE to KeyAction.Backspace,
            KeyRole.SYMBOLS to KeyAction.Symbols,
            KeyRole.SPACE to KeyAction.Space,
            KeyRole.ENTER to KeyAction.Enter,
        )

        expectedActions.forEach { (role, action) ->
            assertEquals(action, key(role).toKeyAction(ShiftState.OFF))
        }
    }

    @Test
    fun emojiDoesNotMapToAKeyAction() {
        assertNull(key(KeyRole.EMOJI).toKeyAction(ShiftState.OFF))
    }

    private fun key(role: KeyRole, label: String = role.name): KeySpec = KeySpec(
        id = role.name.lowercase(),
        label = label,
        role = role,
        accessibilityLabel = role.name,
    )
}
