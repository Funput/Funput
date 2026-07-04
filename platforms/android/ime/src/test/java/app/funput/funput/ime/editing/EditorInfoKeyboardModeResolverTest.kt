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
    fun `plain and flagged text remain standard text mode`() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        assertEquals(KeyboardEditorMode.TEXT, EditorInfoKeyboardModeResolver.resolve(inputType))
    }

    @Test
    fun `non text classes remain standard until their layouts are implemented`() {
        listOf(InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE).forEach { inputType ->
            assertEquals(KeyboardEditorMode.TEXT, EditorInfoKeyboardModeResolver.resolve(inputType))
        }
    }

    private fun resolveTextVariation(variation: Int) =
        EditorInfoKeyboardModeResolver.resolve(InputType.TYPE_CLASS_TEXT or variation)
}
