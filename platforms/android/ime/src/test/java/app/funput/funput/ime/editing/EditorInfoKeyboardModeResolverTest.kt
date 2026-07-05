package app.funput.funput.ime.editing

import android.text.InputType
import app.funput.funput.keyboard.model.KeyboardEditorMode
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorInfoKeyboardModeResolverTest {
    @Test
    fun `native and web email variations resolve to email mode`() {
        listOf(
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
        ).forEach { variation ->
            assertEquals(
                KeyboardEditorMode.EMAIL,
                resolveTextVariation(variation),
            )
        }
    }

    @Test
    fun `URI variation resolves to URL mode`() {
        assertEquals(
            KeyboardEditorMode.URL,
            resolveTextVariation(InputType.TYPE_TEXT_VARIATION_URI),
        )
    }

    @Test
    fun `all text password variations resolve to secure password mode`() {
        listOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        ).forEach { variation ->
            assertEquals(KeyboardEditorMode.PASSWORD, resolveTextVariation(variation))
        }
    }

    @Test
    fun `numeric password resolves to PIN mode even when number flags are present`() {
        val inputType = InputType.TYPE_CLASS_NUMBER or
            InputType.TYPE_NUMBER_VARIATION_PASSWORD or
            InputType.TYPE_NUMBER_FLAG_DECIMAL

        assertEquals(KeyboardEditorMode.PIN, EditorInfoKeyboardModeResolver.resolve(inputType))
    }

    @Test
    fun `plain and flagged text remain standard text mode`() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        assertEquals(KeyboardEditorMode.TEXT, EditorInfoKeyboardModeResolver.resolve(inputType))
    }

    @Test
    fun `phone class resolves to phone mode`() {
        assertEquals(
            KeyboardEditorMode.PHONE,
            EditorInfoKeyboardModeResolver.resolve(InputType.TYPE_CLASS_PHONE),
        )
    }

    @Test
    fun `number flags resolve to the matching numeric mode`() {
        val cases = mapOf(
            0 to KeyboardEditorMode.NUMBER,
            InputType.TYPE_NUMBER_FLAG_DECIMAL to KeyboardEditorMode.NUMBER_DECIMAL,
            InputType.TYPE_NUMBER_FLAG_SIGNED to KeyboardEditorMode.NUMBER_SIGNED,
            InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED to
                KeyboardEditorMode.NUMBER_SIGNED_DECIMAL,
        )
        cases.forEach { (flags, expected) ->
            assertEquals(
                expected,
                EditorInfoKeyboardModeResolver.resolve(InputType.TYPE_CLASS_NUMBER or flags),
            )
        }
    }

    private fun resolveTextVariation(variation: Int) =
        EditorInfoKeyboardModeResolver.resolve(InputType.TYPE_CLASS_TEXT or variation)
}
