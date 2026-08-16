package app.funput.funput.ime.editing

import android.text.InputType
import android.view.inputmethod.EditorInfo
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardEnterAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorInfoPolicyResolverTest {
    @Test
    fun `capitalization flags are preserved for cursor caps queries`() {
        val flags = InputType.TYPE_TEXT_FLAG_CAP_WORDS or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        val policy = resolve(InputType.TYPE_CLASS_TEXT or flags)

        assertEquals(flags, policy.capitalizationModes)
    }

    @Test
    fun `multiline editor keeps its flag and explicit action`() {
        val policy = resolve(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            EditorInfo.IME_ACTION_SEND,
        )

        assertTrue(policy.isMultiline)
        assertEquals(KeyboardEnterAction.Standard.SEND, policy.editorAction.presentation)
    }

    @Test
    fun `host no suggestions flag does not suppress Funput candidates`() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        assertEquals(ImeSuggestionSource.FUNPUT, resolve(inputType).suggestionSource)
        assertTrue(resolve(inputType).showsSuggestionBar)
    }

    @Test
    fun `editor autocomplete does not replace Funput candidates`() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE

        assertEquals(ImeSuggestionSource.FUNPUT, resolve(inputType).suggestionSource)
    }

    @Test
    fun `no personalized learning does not suppress generic candidates`() {
        val policy = resolve(
            InputType.TYPE_CLASS_TEXT,
            EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING,
        )

        assertFalse(policy.allowsPersonalizedLearning)
        assertTrue(policy.allowsPersonalSuggestions)
        assertEquals(ImeSuggestionSource.FUNPUT, policy.suggestionSource)
    }

    @Test
    fun `password overrides capitalization suggestions and learning`() {
        val inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD or
            InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        val policy = resolve(inputType)

        assertEquals(KeyboardEditorMode.PASSWORD, policy.editorMode)
        assertEquals(0, policy.capitalizationModes)
        assertEquals(ImeSuggestionSource.NONE, policy.suggestionSource)
        assertFalse(policy.allowsPersonalizedLearning)
        assertFalse(policy.allowsPersonalSuggestions)
    }

    @Test
    fun `URI enables Funput composition and personal suggestions`() {
        val policy = resolve(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)

        assertEquals(ImeSuggestionSource.FUNPUT, policy.suggestionSource)
        assertTrue(policy.allowsPersonalSuggestions)
    }

    @Test
    fun `plain text enables personal suggestions`() {
        assertTrue(resolve(InputType.TYPE_CLASS_TEXT).allowsPersonalSuggestions)
    }

    @Test
    fun `email enables personal suggestions`() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        assertTrue(resolve(inputType).allowsPersonalSuggestions)
    }

    @Test
    fun `number and phone editors enable personal suggestions`() {
        listOf(InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE).forEach { inputType ->
            val policy = resolve(inputType)

            assertEquals(ImeSuggestionSource.FUNPUT, policy.suggestionSource)
            assertTrue(policy.allowsPersonalSuggestions)
        }
    }

    @Test
    fun `editor package compatibility is included in resolved policy`() {
        assertEquals(
            CompositionRenderMode.COMMITTED,
            resolve(InputType.TYPE_CLASS_TEXT, packageName = "com.facebook.katana").compositionRenderMode,
        )
    }

    @Test
    fun `other editors keep native composing text`() {
        assertEquals(
            CompositionRenderMode.COMPOSING,
            resolve(InputType.TYPE_CLASS_TEXT, packageName = "com.android.chrome").compositionRenderMode,
        )
    }

    private fun resolve(
        inputType: Int,
        imeOptions: Int = EditorInfo.IME_ACTION_NONE,
        packageName: String? = null,
    ) = EditorInfoPolicyResolver.resolve(inputType, imeOptions, packageName = packageName)
}
