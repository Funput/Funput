package app.funput.funput.ime.clipboard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ClipboardEntryTest {
    @Test
    fun `text is preserved exactly`() {
        val text = "  Xin, chào\\bạn\n  "

        assertEquals(text, ClipboardEntry(text = text, sourceToken = "source").text)
    }

    @Test
    fun `empty text and blank source token are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            ClipboardEntry(text = "", sourceToken = "source")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ClipboardEntry(text = "text", sourceToken = "  ")
        }
    }
}
