package app.funput.funput.ime.editing.correction.policy

import android.text.InputType
import app.funput.funput.keyboard.model.KeyboardEditorMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectionFieldPolicyTest {
    private val text = InputType.TYPE_CLASS_TEXT

    @Test
    fun ordinary() {
        assertTrue(CorrectionFieldPolicy.allows(text, KeyboardEditorMode.TEXT))
        assertTrue(CorrectionFieldPolicy.allows(text or InputType.TYPE_TEXT_FLAG_MULTI_LINE, KeyboardEditorMode.TEXT))
    }

    @Test
    fun noSuggestions() {
        assertFalse(CorrectionFieldPolicy.allows(text or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, KeyboardEditorMode.TEXT))
    }

    @Test
    fun passwords() {
        listOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        ).forEach { assertFalse(CorrectionFieldPolicy.allows(text or it, KeyboardEditorMode.TEXT)) }
        assertFalse(CorrectionFieldPolicy.allows(text, KeyboardEditorMode.PASSWORD))
    }

    @Test
    fun addresses() {
        listOf(
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI,
        ).forEach { assertFalse(CorrectionFieldPolicy.allows(text or it, KeyboardEditorMode.TEXT)) }
    }

    @Test
    fun nonText() {
        assertFalse(CorrectionFieldPolicy.allows(InputType.TYPE_CLASS_NUMBER, KeyboardEditorMode.TEXT))
    }
}
