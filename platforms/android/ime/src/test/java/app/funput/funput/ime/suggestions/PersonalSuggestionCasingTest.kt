package app.funput.funput.ime.suggestions

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalSuggestionCasingTest {
    @Test
    fun `applies lowercase titlecase and uppercase from prefix`() {
        assertEquals("việt", PersonalSuggestionCasing.apply("VIỆT", "vi"))
        assertEquals("Việt", PersonalSuggestionCasing.apply("việt", "Vi"))
        assertEquals("VIỆT", PersonalSuggestionCasing.apply("việt", "VI"))
    }

    @Test
    fun `a prediction takes its case from shift, having no prefix to take it from`() {
        assertEquals("chào", PersonalSuggestionCasing.apply("chào", "", capitalized = false))
        assertEquals("Chào", PersonalSuggestionCasing.apply("chào", "", capitalized = true))
    }
}
