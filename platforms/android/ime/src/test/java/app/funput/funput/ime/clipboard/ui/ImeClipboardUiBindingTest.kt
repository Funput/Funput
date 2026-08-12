package app.funput.funput.ime.clipboard.ui

import app.funput.funput.ime.R
import app.funput.funput.ime.clipboard.controller.ClipboardPasteResult
import app.funput.funput.ime.clipboard.policy.ClipboardOffer
import app.funput.funput.ime.clipboard.policy.ClipboardOfferKind
import app.funput.funput.keyboard.KeyboardClipboardHint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImeClipboardUiBindingTest {
    @Test
    fun `offer mapping contains metadata only`() {
        assertEquals(KeyboardClipboardHint.TEXT, offer(ClipboardOfferKind.TEXT).toHint())
        assertEquals(KeyboardClipboardHint.LINK, offer(ClipboardOfferKind.LINK).toHint())
        assertEquals(KeyboardClipboardHint.SENSITIVE, offer(ClipboardOfferKind.SENSITIVE).toHint())
        assertNull((null as ClipboardOffer?).toHint())
    }

    @Test
    fun `only actionable failures produce messages`() {
        assertEquals(R.string.clipboard_paste_too_large, ClipboardPasteResult.TOO_LARGE.messageResource())
        listOf(
            ClipboardPasteResult.EMPTY,
            ClipboardPasteResult.UNSUPPORTED,
            ClipboardPasteResult.UNAVAILABLE,
        ).forEach { assertEquals(R.string.clipboard_paste_failed, it.messageResource()) }
        listOf(
            ClipboardPasteResult.PASTED,
            ClipboardPasteResult.BLOCKED,
            ClipboardPasteResult.BUSY,
            ClipboardPasteResult.CHANGED,
        ).forEach { assertNull(it.messageResource()) }
    }

    private fun offer(kind: ClipboardOfferKind) = ClipboardOffer(kind, "token")
}
