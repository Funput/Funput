package app.funput.funput.keyboard.suggestion

import app.funput.funput.keyboard.KeyboardClipboardHint
import app.funput.funput.keyboard.KeyboardClipboardOffer
import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.layout.ResolvedSuggestionBar
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardOfferChipTest {
    private val offer = KeyboardClipboardOffer(KeyboardClipboardHint.TEXT, preview = "xin chào")

    @Test
    fun `shows on an empty band that is wide enough`() {
        assertTrue(visible(offer))
        // A 320dp phone at the default size leaves the band about 149dp between the toolbar keys.
        assertTrue(visible(offer, bandWidthDp = 149f, density = 2.75f))
        assertTrue(visible(offer, bandWidthDp = ClipboardOfferChip.MinWidthDp))
    }

    @Test
    fun `yields to candidates typing a missing offer and a narrow band`() {
        assertFalse(visible(offer, suggestionCount = 1))
        assertFalse(visible(offer.copy(showsChip = false)))
        assertFalse(visible(null))
        assertFalse(visible(offer, bandWidthDp = ClipboardOfferChip.MinWidthDp - 1f))
        assertFalse(ClipboardOfferChip.isVisible(offer, 0, null, 1f))
    }

    @Test
    fun `stays off a band that takes no taps`() {
        assertFalse(visible(offer, suggestionsEnabled = false))
    }

    @Test
    fun `sits at the start of the band and never outgrows it`() {
        val band = KeyBounds(40f, 8f, 340f, 42f)
        assertEquals(KeyBounds(40f, 8f, 160f, 42f), ClipboardOfferChip.plateBounds(band, contentWidth = 120f))
        assertEquals(band, ClipboardOfferChip.plateBounds(band, contentWidth = 900f))
    }

    @Test
    fun `never previews a sensitive clip`() {
        val sensitive = KeyboardClipboardOffer(KeyboardClipboardHint.SENSITIVE, preview = "mật khẩu")
        assertNull(sensitive.visiblePreview)
        assertNull(offer.copy(preview = "  ").visiblePreview)
        assertEquals("xin chào", offer.visiblePreview)
    }

    private fun visible(
        offer: KeyboardClipboardOffer?,
        suggestionCount: Int = 0,
        bandWidthDp: Float = 180f,
        density: Float = 1f,
        suggestionsEnabled: Boolean = true,
    ): Boolean {
        val band = KeyBounds(0f, 0f, bandWidthDp * density, 34f * density)
        val emoji = ResolvedKey(KeySpec("emoji", "", KeyRole.EMOJI, accessibilityLabel = "Emoji"), band)
        val bar = ResolvedSuggestionBar(
            bounds = band, suggestionsBounds = band, systemInputMethodKey = null, placementKey = null,
            emojiKey = emoji, suggestionsEnabled = suggestionsEnabled,
        )
        return ClipboardOfferChip.isVisible(offer, suggestionCount, bar, density)
    }
}
