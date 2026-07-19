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
}
