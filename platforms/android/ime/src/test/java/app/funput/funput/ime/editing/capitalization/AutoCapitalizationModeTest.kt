package app.funput.funput.ime.editing.capitalization

import android.text.TextUtils
import app.funput.funput.ime.editing.EditorInfoPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoCapitalizationModeTest {
    @Test
    fun `an editor that asks for nothing still gets sentences`() {
        assertEquals(AutoCapitalizationMode.SENTENCES, mode(requested = 0))
    }

    @Test
    fun `the preference silences the convenience modes`() {
        assertEquals(AutoCapitalizationMode.NONE, mode(requested = 0, preference = false))
        assertEquals(
            AutoCapitalizationMode.NONE,
            mode(TextUtils.CAP_MODE_SENTENCES, preference = false),
        )
        assertEquals(
            AutoCapitalizationMode.NONE,
            mode(TextUtils.CAP_MODE_WORDS, preference = false),
        )
    }

    @Test
    fun `a field demanding all caps outranks a disabled preference`() {
        assertEquals(
            AutoCapitalizationMode.ALL_CHARACTERS,
            mode(TextUtils.CAP_MODE_CHARACTERS, preference = false),
        )
    }

    @Test
    fun `a field asking for words gets words`() {
        assertEquals(AutoCapitalizationMode.WORDS, mode(TextUtils.CAP_MODE_WORDS))
    }

    @Test
    fun `characters outrank words when an editor asks for both`() {
        val requested = TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS

        assertEquals(AutoCapitalizationMode.ALL_CHARACTERS, mode(requested))
    }

    @Test
    fun `a field that never holds prose capitalizes nothing`() {
        val policy = EditorInfoPolicy.Default.copy(
            capitalizationModes = TextUtils.CAP_MODE_CHARACTERS,
            allowsAutoCapitalization = false,
        )

        assertEquals(AutoCapitalizationMode.NONE, policy.autoCapitalizationMode(true))
    }

    private fun mode(requested: Int, preference: Boolean = true) =
        EditorInfoPolicy.Default
            .copy(capitalizationModes = requested)
            .autoCapitalizationMode(preference)
}
