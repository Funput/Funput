package app.funput.funput.ime.clipboard.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardOfferPreviewTest {
    @Test
    fun `short text becomes one trimmed line`() {
        assertEquals("Funput là bộ gõ tiếng Việt", ClipboardOfferPreview.of("  Funput là\n\tbộ gõ   tiếng Việt \n"))
        assertEquals("https://funput.app", ClipboardOfferPreview.of("https://funput.app"))
    }

    @Test
    fun `nothing visible means no preview`() {
        assertNull(ClipboardOfferPreview.of(""))
        assertNull(ClipboardOfferPreview.of(" \n\t "))
    }

    @Test
    fun `long text is cut by code points and marked`() {
        val preview = requireNotNull(ClipboardOfferPreview.of("😀".repeat(200)))
        assertEquals("😀".repeat(ClipboardOfferPreview.MaxCodePoints) + "…", preview)
    }

    @Test
    fun `a huge clip is only read at its opening`() {
        val preview = requireNotNull(ClipboardOfferPreview.of("a ".repeat(50_000)))
        assertTrue(preview.endsWith("…"))
        assertTrue(preview.codePointCount(0, preview.length) <= ClipboardOfferPreview.MaxCodePoints + 1)
    }

    @Test
    fun `an opening that ends inside a surrogate pair drops the stray half`() {
        // The 400 characters examined end on the first half of the emoji.
        assertEquals("a…", ClipboardOfferPreview.of("a" + " ".repeat(398) + "😀 tail"))
    }
}
