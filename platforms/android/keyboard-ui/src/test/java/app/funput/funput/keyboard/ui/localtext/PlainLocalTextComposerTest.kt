package app.funput.funput.keyboard.ui.localtext

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlainLocalTextComposerTest {
    @Test fun `keeps keys as typed and never composes`() {
        val field = PlainLocalTextComposer()
        field.apply(LocalTextEdit.Text("a"))
        field.apply(LocalTextEdit.Text("s"))
        assertEquals("as", field.text)
        assertNull(field.inputMethod)
    }

    @Test fun `drops leading and repeated spaces`() {
        val field = PlainLocalTextComposer()
        listOf(LocalTextEdit.Space, LocalTextEdit.Text("m"), LocalTextEdit.Space, LocalTextEdit.Space)
            .forEach(field::apply)
        field.apply(LocalTextEdit.Text("a"))
        assertEquals("m a", field.text)
    }

    @Test fun `deletes one code point at a time and resets`() {
        val field = PlainLocalTextComposer()
        field.apply(LocalTextEdit.DeleteBackward)
        field.apply(LocalTextEdit.Text("đ😀"))
        field.apply(LocalTextEdit.DeleteBackward)
        assertEquals("đ", field.text)
        field.reset()
        assertEquals("", field.text)
    }
}
