package app.funput.funput.ime.editing.correction.preview

/** What the suggestion bar shows about typo correction, when anything. */
internal sealed interface CorrectionPreview {
    /** The word being typed is dead: [typed] as it stands, and what Space would make of it. */
    data class Offered(val typed: String, val candidates: List<String>) : CorrectionPreview

    /** Space committed [typed] undecided; the bar still offers [candidates] until the next key. */
    data class Parked(val typed: String, val candidates: List<String>) : CorrectionPreview

    /** A repair landed; [typed] is what Backspace, or a tap, would put back. */
    data class Applied(val typed: String) : CorrectionPreview
}
