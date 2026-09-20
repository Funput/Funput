package app.funput.funput.ime.suggestions

import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The same table iOS asserts in `SuggestionCasingTests`. Two hand-written copies of one
 * rule already drifted apart once — a lone capital meant ALL CAPS on both, which neither
 * the shortcut expander nor anybody typing "Việt" agreed with.
 */
class PersonalSuggestionCasingTest {
    @Test
    fun `the prefix sets the case`() {
        // A lowercase prefix leaves the word alone, which is how iPhone survives.
        assertEquals("VIỆT", PersonalSuggestionCasing.apply("VIỆT", "vi"))
        assertEquals("iPhone", PersonalSuggestionCasing.apply("iPhone", "ip"))
        assertEquals("IPHONE", PersonalSuggestionCasing.apply("iPhone", "IP"))
        assertEquals("Việt", PersonalSuggestionCasing.apply("việt", "Vi"))
        assertEquals("VIỆT", PersonalSuggestionCasing.apply("việt", "VI"))
    }

    @Test
    fun `one capital letter starts a sentence rather than shouting`() {
        assertEquals("Việt", PersonalSuggestionCasing.apply("việt", "V1"))
        // Digits are skipped, so the first letter is what decides.
        assertEquals("việt", PersonalSuggestionCasing.apply("việt", "1v"))
    }

    @Test
    fun `a deliberately mixed prefix speaks for itself`() {
        assertEquals("việt", PersonalSuggestionCasing.apply("việt", "VNa"))
        assertEquals("iPhone", PersonalSuggestionCasing.apply("iPhone", "iOS"))
    }

    @Test
    fun `shift outranks a prefix the user typed in lowercase`() {
        assertEquals("Việt", PersonalSuggestionCasing.apply("việt", "vi", ShiftState.ON))
        assertEquals("VIỆT", PersonalSuggestionCasing.apply("việt", "vi", ShiftState.CAPS_LOCK))
    }

    @Test
    fun `a prediction takes its case from shift, having no prefix to take it from`() {
        assertEquals("chào", PersonalSuggestionCasing.apply("chào", "", ShiftState.OFF))
        assertEquals("Chào", PersonalSuggestionCasing.apply("chào", "", ShiftState.ON))
        assertEquals("CHÀO", PersonalSuggestionCasing.apply("chào", "", ShiftState.CAPS_LOCK))
    }
}
