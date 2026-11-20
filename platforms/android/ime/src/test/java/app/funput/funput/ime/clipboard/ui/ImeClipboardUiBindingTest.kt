package app.funput.funput.ime.clipboard.ui

import app.funput.funput.ime.R
import app.funput.funput.ime.clipboard.controller.ClipboardPasteResult
import app.funput.funput.ime.clipboard.policy.ClipboardOffer
import app.funput.funput.ime.clipboard.policy.ClipboardOfferKind
import app.funput.funput.keyboard.KeyboardClipboardHint
import app.funput.funput.keyboard.KeyboardClipboardOffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImeClipboardUiBindingTest {
    @Test
    fun `offer mapping keeps the kind and carries the preview and chip state`() {
        assertEquals(KeyboardClipboardHint.TEXT, offer(ClipboardOfferKind.TEXT).toKeyboardOffer()?.hint)
        assertEquals(KeyboardClipboardHint.LINK, offer(ClipboardOfferKind.LINK).toKeyboardOffer()?.hint)
        assertEquals(KeyboardClipboardHint.SENSITIVE, offer(ClipboardOfferKind.SENSITIVE).toKeyboardOffer()?.hint)
        assertNull((null as ClipboardOffer?).toKeyboardOffer())
        assertEquals(
            KeyboardClipboardOffer(KeyboardClipboardHint.TEXT, "xin chào", showsChip = false),
            offer(ClipboardOfferKind.TEXT).copy(preview = "xin chào").toKeyboardOffer(showsChip = false),
        )
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
