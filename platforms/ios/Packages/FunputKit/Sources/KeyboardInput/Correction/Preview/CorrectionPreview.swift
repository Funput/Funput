/// What the suggestion bar shows about typo correction for the word under the caret.
///
/// Set by `KeyboardInputCoordinator.refreshCorrectionPreview()` after every key that
/// can change it, and read by the host when it publishes the bar. Words, not
/// indices: the bar and the engine can disagree about order, but never about text.
public enum CorrectionPreview: Equatable, Sendable {
    /// The word has dead-ended and the engine can reach a repair. `typed` is the
    /// word as the screen shows it; `candidates` is the best repair, then a second
    /// one when the two are too close to call. Space applies the first; tapping
    /// `typed` keeps it.
    case offered(typed: String, candidates: [String])
    /// A repair was just applied, and one tap takes it back: `undoText` is the word
    /// that would come back.
    case applied(undoText: String)
}

/// The correction the boundary last applied, kept while the engine still offers to
/// undo it. `shown` is what the screen showed when Space was pressed and what an
/// undo puts back; `composed` is the syllable the user watched while typing it.
struct AppliedCorrection: Equatable {
    let shown: String
    let composed: String
}
