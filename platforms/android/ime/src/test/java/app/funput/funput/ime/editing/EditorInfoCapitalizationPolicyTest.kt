package app.funput.funput.ime.editing

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the editor asks for, and which editors are asked about at all. */
class EditorInfoCapitalizationPolicyTest {
    @Test
    fun `capitalization flags are preserved for the mode resolver`() {
        val flags = InputType.TYPE_TEXT_FLAG_CAP_WORDS or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        assertEquals(flags, resolve(InputType.TYPE_CLASS_TEXT or flags).capitalizationModes)
    }

    @Test
    fun `prose fields allow auto capitalization`() {
        assertTrue(resolve(InputType.TYPE_CLASS_TEXT).allowsAutoCapitalization)
        assertTrue(
            resolve(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE)
                .allowsAutoCapitalization,
        )
    }

    @Test
    fun `address and secret fields do not`() {
        listOf(
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
        ).forEach { variation ->
            assertFalse(resolve(InputType.TYPE_CLASS_TEXT or variation).allowsAutoCapitalization)
        }
    }

    @Test
    fun `non text editors do not`() {
        listOf(InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE).forEach { inputType ->
            assertFalse(resolve(inputType).allowsAutoCapitalization)
        }
    }

    private fun resolve(inputType: Int) =
        EditorInfoPolicyResolver.resolve(inputType, EditorInfo.IME_ACTION_NONE)
}
