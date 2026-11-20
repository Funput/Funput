package app.funput.funput.keyboard.suggestion

import app.funput.funput.keyboard.KeyboardClipboardOffer
import app.funput.funput.keyboard.layout.ResolvedSuggestionBar

/**
 * The paste offer shown on the suggestion band while it has no candidates to show.
 *
 * Drawing, hit testing and accessibility all ask [isVisible], so the chip can never be drawn
 * where it cannot be tapped, or announced where it is not drawn.
 */
internal object ClipboardOfferChip {
    const val TargetId = "clipboard-offer"

    /** Narrowest band, in dp, that still fits the glyph and a few words beside it. */
    const val MinWidthDp = 96f

    fun isVisible(
        offer: KeyboardClipboardOffer?,
        suggestionCount: Int,
        bar: ResolvedSuggestionBar?,
        density: Float,
    ): Boolean = offer != null && offer.showsChip && suggestionCount == 0 &&
        // A band with suggestions switched off is not a tap target at all.
        bar != null && bar.suggestionsEnabled &&
        bar.suggestionsBounds.width >= MinWidthDp * density
}
