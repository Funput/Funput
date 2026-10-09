package app.funput.funput.ime.speech.editor

import android.text.InputType
import app.funput.funput.ime.editing.CompositionRenderMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechSpikeEditorPolicyTest {
    @Test fun sentenceAndSearchFieldsPermitSpeech() {
        val variants = listOf(InputType.TYPE_TEXT_VARIATION_NORMAL, InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE, InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT)
        variants.forEach { variation ->
            assertTrue(allows(InputType.TYPE_CLASS_TEXT or variation))
        }
    }

    @Test fun secretsAddressesAndNonTextFieldsDoNotPermitSpeech() {
        val variants = listOf(InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)
        variants.forEach { variation ->
            assertFalse(allows(InputType.TYPE_CLASS_TEXT or variation))
        }
        listOf(InputType.TYPE_NULL, InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE,
            InputType.TYPE_CLASS_DATETIME).forEach { assertFalse(allows(it)) }
    }

    @Test fun keyEventCompatibilityHostsCannotCommitExternalVoiceText() {
        assertFalse(SpeechEditorPolicy.allows(InputType.TYPE_CLASS_TEXT, CompositionRenderMode.KEY_EVENT))
        assertTrue(SpeechEditorPolicy.allows(InputType.TYPE_CLASS_TEXT, CompositionRenderMode.COMMITTED))
    }

    private fun allows(inputType: Int) = SpeechEditorPolicy.allows(inputType, CompositionRenderMode.COMPOSING)
}
