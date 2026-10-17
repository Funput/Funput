package app.funput.funput.ime.editing.correction

/**
 * A repair the engine could not decide on at Space, left on the bar until the next key.
 * [shown] is the word as the document shows it, [written] that word plus the boundary as
 * it went into the document, and [candidates] what the bar offers in its place.
 */
internal data class ParkedCorrection(val shown: String, val written: String, val candidates: List<String>)

/**
 * A repair that landed, while Backspace can still take it back: [typed] is the word as the
 * document showed it, [composed] the syllable the engine had built from it, and [corrected]
 * the repair plus its boundary, exactly as it now sits before the caret.
 */
internal data class AppliedCorrection(val typed: String, val composed: String, val corrected: String)

/** Why a word with candidates was left alone, for the end-of-session report. */
internal class CorrectionDeclines {
    /** The user's own words or the shipped lists knew it. */
    var recognized = 0

    /** The field asked for no corrections. */
    var fieldRefused = 0

    /** The user tapped the word as typed on the bar. */
    var refused = 0
}
