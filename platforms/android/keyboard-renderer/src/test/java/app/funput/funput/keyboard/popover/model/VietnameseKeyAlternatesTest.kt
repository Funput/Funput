package app.funput.funput.keyboard.popover.model

import app.funput.funput.keyboard.layout.KeyboardLayoutResolver
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.model.KeyboardLayoutMode
import app.funput.funput.keyboard.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VietnameseKeyAlternatesTest {
    @Test
    fun `catalog matches iOS order`() {
        assertEquals(chars("aáàảãạăắằẳẵặâấầẩẫậ"), texts('a'))
        assertEquals(chars("eéèẻẽẹêếềểễệ"), texts('e'))
        assertEquals(chars("iíìỉĩị"), texts('i'))
        assertEquals(chars("oóòỏõọôốồổỗộơớờởỡợ"), texts('o'))
        assertEquals(chars("uúùủũụưứừửữự"), texts('u'))
        assertEquals(chars("yýỳỷỹỵ"), texts('y'))
        assertEquals(listOf("d", "đ"), texts('d'))
        assertTrue(texts('b').isEmpty())
    }

    @Test
    fun `shifted values are uppercase`() {
        assertEquals(
            chars("OÓÒỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢ"),
            VietnameseKeyAlternates.valuesFor('o').map { it.textFor(ShiftState.ON) },
        )
    }

    @Test
    fun `only text and search layouts expose alternates`() {
        KeyboardInputMethod.entries.forEach { method ->
            assertTrue(alternates(method, KeyboardEditorMode.TEXT).isNotEmpty())
            assertTrue(alternates(method, KeyboardEditorMode.SEARCH).isNotEmpty())
            listOf(
                KeyboardEditorMode.EMAIL,
                KeyboardEditorMode.URL,
                KeyboardEditorMode.PASSWORD,
            ).forEach { mode -> assertTrue(alternates(method, mode).isEmpty()) }
        }
    }

    private fun texts(character: Char) =
        VietnameseKeyAlternates.valuesFor(character).map(KeyAlternate::text)

    private fun chars(value: String) = value.map(Char::toString)

    private fun alternates(method: KeyboardInputMethod, mode: KeyboardEditorMode) =
        KeyboardLayoutResolver.resolve(method, KeyboardLayoutMode.LETTERS, mode)
            .rows.flatMap { it.keys }.flatMap { it.alternates }
}
